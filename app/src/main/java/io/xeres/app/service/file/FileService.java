/*
 * Copyright (c) 2023-2024 by David Gerber - https://zapek.com
 *
 * This file is part of Xeres.
 *
 * Xeres is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Xeres is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Xeres.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.xeres.app.service.file;

import io.xeres.app.configuration.DataDirConfiguration;
import io.xeres.app.crypto.hash.sha1.Sha1MessageDigest;
import io.xeres.app.database.model.file.File;
import io.xeres.app.database.model.file.FileDownload;
import io.xeres.app.database.model.share.Share;
import io.xeres.app.database.repository.FileDownloadRepository;
import io.xeres.app.database.repository.FileRepository;
import io.xeres.app.database.repository.ShareRepository;
import io.xeres.app.service.notification.file.FileNotificationService;
import io.xeres.common.id.Sha1Sum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalAmount;
import java.util.*;
import java.util.stream.Collectors;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;

@Service
public class FileService
{
	private static final Logger log = LoggerFactory.getLogger(FileService.class);

	public static final String DOWNLOAD_EXTENSION = ".xrsdownload";

	private static final TemporalAmount SCAN_DELAY = Duration.ofMinutes(10); // Delay between shares scan

	static final int SMALL_FILE_SIZE = 1024 * 16; // 16 KB

	private final FileNotificationService fileNotificationService;

	private final ShareRepository shareRepository;

	private final FileRepository fileRepository;

	private final FileDownloadRepository fileDownloadRepository;

	private final HashBloomFilter bloomFilter;

	private static final String[] ignoredSuffixes = {
			".bak",
			".sys",
			".com",
			".class",
			".obj",
			".o",
			".tmp",
			".temp",
			".cache",
			DOWNLOAD_EXTENSION,
			"~"
	};

	private static final String[] ignoredPrefixes = {
			"thumbs",
			"temp."
	};

	public FileService(FileNotificationService fileNotificationService, ShareRepository shareRepository, FileRepository fileRepository, FileDownloadRepository fileDownloadRepository, DataDirConfiguration dataDirConfiguration)
	{
		this.fileNotificationService = fileNotificationService;
		this.shareRepository = shareRepository;
		this.fileRepository = fileRepository;
		this.fileDownloadRepository = fileDownloadRepository;
		bloomFilter = new HashBloomFilter(dataDirConfiguration.getDataDir(), 10_000, 0.01d); // XXX: parameters will need experimenting, especially the max files (yes it can be extended, but not reduced)
		updateBloomFilter();
	}

	public void addShare(Share share)
	{
		saveFullPath(share.getFile());
		if (shareRepository.findByName(share.getName()).isPresent())
		{
			return;
		}
		shareRepository.save(share);
	}

	/**
	 * This is used for migration only.
	 */
	@Transactional
	public void encryptAllHashes()
	{
		fileRepository.findAll().forEach(file -> {
			if (file.getHash() != null)
			{
				file.setEncryptedHash(encryptHash(file.getHash()));
			}
		});
	}

	@Transactional
	public void checkForSharesToScan()
	{
		var sharesToScan = shareRepository.findAll(Sort.by(Sort.Order.by("lastScanned").nullsFirst()).ascending());

		log.debug("Shares to scan: {}", sharesToScan);
		var now = Instant.now();
		sharesToScan.stream()
				.filter(share -> share.getLastScanned() == null || share.getLastScanned().isBefore(now.minus(SCAN_DELAY)))
				.findFirst().ifPresent(share -> {
					log.debug("Scanning: {}", share);
					share.setLastScanned(now);
					shareRepository.save(share);
					scanShare(share);
				});
	}

	@Transactional
	public void synchronize(List<Share> shares)
	{
		emptyIfNull(shares).forEach(share -> {
			saveFullPath(share.getFile());
			shareRepository.save(share); // XXX: not needed I think... it's a transactional
		});

		var ids = shares.stream()
				.map(Share::getId)
				.filter(id -> id != 0)
				.collect(Collectors.toSet());

		emptyIfNull(getShares()).forEach(share -> {
			if (!ids.contains(share.getId()))
			{
				// XXX: make sure no indexing process is handling this, it will have to be aborted first then
				var sharedDirectory = share.getFile();
				shareRepository.delete(share);
				fileRepository.delete(sharedDirectory);
			}
		});
	}

	public List<Share> getShares()
	{
		return shareRepository.findAll();
	}

	public Map<Long, String> getFilesMapFromShares(List<Share> shares)
	{
		return shares.stream()
				.collect(Collectors.toMap(Share::getId, share -> toPath(getFullPath(share.getFile()))));
	}

	private String toPath(List<File> files)
	{
		return files.stream()
				.map(file -> file.getName().endsWith(":\\") ? file.getName().substring(0, file.getName().length() - 1) : file.getName()) // On Windows, C:\ -> C: to avoid double file separators
				.collect(Collectors.joining(java.io.File.separator));
	}

	public Optional<File> findFileByHash(Sha1Sum hash)
	{
		return fileRepository.findByHash(hash);
	}

	public Optional<File> findFileByEncryptedHash(Sha1Sum encryptedHash)
	{
		if (bloomFilter.mightContain(encryptedHash))
		{
			return fileRepository.findByEncryptedHash(encryptedHash);
		}
		return Optional.empty();
	}

	public Optional<Path> findFilePathByHash(Sha1Sum hash)
	{
		Objects.requireNonNull(hash);

		return findFileByHash(hash).map(this::getFilePath);
	}

	public Optional<Path> findFilePathByEncryptedHash(Sha1Sum encryptedHash)
	{
		Objects.requireNonNull(encryptedHash);

		return findFileByEncryptedHash(encryptedHash).map(this::getFilePath);
	}

	public List<File> searchFiles(String name)
	{
		return fileRepository.findAllByNameContainingIgnoreCase(name);
	}

	public long addDownload(String name, Sha1Sum hash, long size)
	{
		if (fileDownloadRepository.findByHash(hash).isPresent())
		{
			return 0L;
		}

		var fileDownload = new FileDownload();
		fileDownload.setName(name);
		fileDownload.setHash(hash);
		fileDownload.setSize(size);
		var saved = fileDownloadRepository.save(fileDownload);
		return saved.getId();
	}

	@Transactional
	public void suspendDownload(Sha1Sum hash, BitSet chunkMap)
	{
		fileDownloadRepository.findByHashAndCompletedFalse(hash).ifPresent(fileDownload -> fileDownload.setChunkMap(chunkMap));
	}

	@Transactional
	public void markDownloadAsCompleted(Sha1Sum hash)
	{
		fileDownloadRepository.findByHash(hash).ifPresent(fileDownload -> fileDownload.setCompleted(true));
	}

	public Optional<Sha1Sum> findByPath(Path path)
	{
		var candidates = fileRepository.findAllByName(path.getFileName().toString());
		for (File candidate : candidates)
		{
			if (getFullPathAsString(candidate).equals(path.toString()))
			{
				return Optional.of(candidate.getHash());
			}
		}
		return Optional.empty();
	}

	public static Sha1Sum encryptHash(Sha1Sum hash)
	{
		var digest = new Sha1MessageDigest();
		digest.update(hash.getBytes());
		return digest.getSum();
	}

	private void saveFullPath(File file)
	{
		var tree = getFullPath(file);
		fileRepository.saveAll(tree);
	}

	private List<File> getFullPath(File file)
	{
		List<File> tree = new ArrayList<>();

		tree.add(file);
		while (file.getParent() != null)
		{
			tree.add(file.getParent());
			file = file.getParent();
		}
		Collections.reverse(tree);
		tree.forEach(fileToUpdate -> fileRepository.findByNameAndParentName(fileToUpdate.getName(), fileToUpdate.getParent() != null ? fileToUpdate.getParent().getName() : null).ifPresent(fileFound -> fileToUpdate.setId(fileFound.getId())));
		return tree;
	}

	private String getFullPathAsString(File file)
	{
		return toPath(getFullPath(file));
	}

	void scanShare(Share share)
	{
		try
		{
			var ioBuffer = new byte[SMALL_FILE_SIZE];
			fileNotificationService.startScanning(share);
			File directory = share.getFile();
			var directoryPath = getFilePath(directory);
			var visitor = new TrackingFileVisitor(fileRepository, directory)
			{
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
				{
					Objects.requireNonNull(file);
					Objects.requireNonNull(attrs);
					if (isIndexableFile(file, attrs))
					{
						indexFile(file, attrs);
					}
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
				{
					Objects.requireNonNull(dir);
					Objects.requireNonNull(attrs);
					if (isIndexableDirectory(dir, attrs))
					{
						indexDirectory(dir, attrs);
						return FileVisitResult.CONTINUE;
					}
					else
					{
						return FileVisitResult.SKIP_SUBTREE;
					}
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc)
				{
					Objects.requireNonNull(dir);
					super.postVisitDirectory(dir, exc);
					if (exc != null)
					{
						log.debug("Failed to fully scan directory {}: {}", dir, exc.getMessage());
					}
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(Path file, IOException exc)
				{
					Objects.requireNonNull(file);
					log.debug("Visiting file {} failed: {}", file, exc.getMessage());
					return FileVisitResult.CONTINUE;
				}

				private void indexFile(Path file, BasicFileAttributes attrs)
				{
					var currentFile = fileRepository.findByNameAndParent(file.getFileName().toString(), getCurrentDirectory()).orElseGet(() -> File.createFile(getCurrentDirectory(), file.getFileName().toString(), attrs.size(), null));
					var lastModified = attrs.lastModifiedTime().toInstant();
					log.debug("Checking file {}, modification time: {}", file, lastModified);
					if (currentFile.getModified() == null || lastModified.isAfter(currentFile.getModified()))
					{
						log.debug("Current file in database, modified: {}", currentFile.getModified());
						var hash = calculateFileHash(file, ioBuffer);
						currentFile.setHash(hash);
						currentFile.setEncryptedHash(encryptHash(hash));
						currentFile.setModified(lastModified);
						fileRepository.save(currentFile);
						setChanged();
					}
				}

				private void indexDirectory(Path dir, BasicFileAttributes attrs)
				{
					super.preVisitDirectory(dir, attrs);
					log.debug("Entering directory {}", dir);
					var directory = getCurrentDirectory();
					if (fileRepository.findByNameAndParent(directory.getName(), directory.getParent()).isEmpty())
					{
						fileRepository.save(directory);
					}
				}
			};
			Files.walkFileTree(directoryPath, visitor);
			directory.setModified(Files.getLastModifiedTime(directoryPath).toInstant());
			fileRepository.save(directory);

			if (visitor.foundChanges())
			{
				updateBloomFilter();
			}
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
		finally
		{
			fileNotificationService.stopScanning();
		}
	}

	private Path getFilePath(File file)
	{
		if (file.hasParent())
		{
			return getFilePath(file.getParent()).resolve(file.getName());
		}
		return Path.of(file.getName());
	}

	private boolean isIndexableFile(Path file, BasicFileAttributes attrs)
	{
		if (attrs.isRegularFile() && attrs.size() > 0)
		{
			var fileName = file.getFileName().toString();
			return !isIgnoredFile(fileName);
		}
		return false;
	}

	private boolean isIndexableDirectory(Path directory, BasicFileAttributes attrs)
	{
		if (attrs.isDirectory())
		{
			var directoryName = directory.getFileName().toString();
			return !isIgnoredDirectory(directoryName);
		}
		return false;
	}

	private boolean isIgnoredFile(String fileName)
	{
		fileName = fileName.toLowerCase(Locale.ROOT);

		for (var ignoredSuffix : ignoredSuffixes)
		{
			if (fileName.endsWith(ignoredSuffix))
			{
				return true;
			}
		}

		for (var ignoredPrefix : ignoredPrefixes)
		{
			if (fileName.startsWith(ignoredPrefix))
			{
				return true;
			}
		}
		return false;
	}

	private boolean isIgnoredDirectory(String dirName)
	{
		return dirName.startsWith(".");
	}

	public Sha1Sum calculateFileHash(Path path)
	{
		var ioBuffer = new byte[SMALL_FILE_SIZE];
		return calculateFileHash(path, ioBuffer);
	}

	Sha1Sum calculateFileHash(Path path, byte[] ioBuffer)
	{
		log.debug("Calculating file hash of file {}", path);
		try
		{
			var size = Files.size(path);

			if (size == 0)
			{
				log.debug("File is empty, ignoring");
				return null; // We ignore empty files
			}
			else if (size > SMALL_FILE_SIZE)
			{
				return calculateLargeFileHash(path);
			}
			else
			{
				return calculateSmallFileHash(path, ioBuffer);
			}
		}
		catch (IOException e)
		{
			log.warn("Error while trying to compute hash of file {}", path, e);
			return null;
		}
	}

	private Sha1Sum calculateLargeFileHash(Path path) throws IOException
	{
		try (var fc = FileChannel.open(path, StandardOpenOption.READ)) // ExtendedOpenOption.DIRECT is useless for memory mapped files
		{
			fileNotificationService.startScanningFile(path);
			var md = new Sha1MessageDigest();

			var size = fc.size();
			var offset = 0L;

			while (size > 0)
			{
				var bufferSize = Math.min(size, Integer.MAX_VALUE);
				var buffer = fc.map(FileChannel.MapMode.READ_ONLY, offset, bufferSize);

				md.update(buffer);
				offset += bufferSize;
				size -= bufferSize;
			}
			return md.getSum();
		}
		finally
		{
			fileNotificationService.stopScanningFile();
		}
	}

	private Sha1Sum calculateSmallFileHash(Path path, byte[] ioBuffer) throws IOException
	{
		try (var ios = new FileInputStream(path.toFile()))
		{
			fileNotificationService.startScanningFile(path);
			var md = new Sha1MessageDigest();
			int read;

			while ((read = ios.read(ioBuffer)) > 0)
			{
				md.update(ioBuffer, 0, read);
			}
			return md.getSum();
		}
		finally
		{
			fileNotificationService.stopScanningFile();
		}
	}

	private void updateBloomFilter()
	{
		// XXX: extend the bloom filter if needed
		bloomFilter.clear();
		fileRepository.findAll().forEach(file -> bloomFilter.add(file.getEncryptedHash()));
	}
}
