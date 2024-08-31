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
import io.xeres.app.database.model.file.FileFakes;
import io.xeres.app.database.model.share.ShareFakes;
import io.xeres.app.database.repository.FileRepository;
import io.xeres.app.database.repository.ShareRepository;
import io.xeres.app.service.notification.file.FileNotificationService;
import io.xeres.common.id.Id;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class FileServiceTest
{
	@Mock
	private FileNotificationService fileNotificationService;

	@Mock
	private HashBloomFilter hashBloomFilter;

	@Mock
	private DataDirConfiguration dataDirConfiguration;

	@Mock
	private FileRepository fileRepository;

	@Mock
	private ShareRepository shareRepository;

	@InjectMocks
	private FileService fileService;

	@BeforeAll
	public static void setErrorLogging()
	{
		LoggingSystem.get(ClassLoader.getSystemClassLoader()).setLogLevel("io.xeres", LogLevel.DEBUG);
	}

	@Test
	void HashFile_Success() throws URISyntaxException
	{
		var ioBuffer = new byte[FileService.SMALL_FILE_SIZE];

		// mmap (> 16 KB file)
		var hash = fileService.calculateFileHash(Path.of(Objects.requireNonNull(getClass().getResource("/image/leguman.jpg")).toURI()), ioBuffer);
		assertNotNull(hash);
		assertEquals("0f02355b1b1e9a22801dddd85ded59fe7301698d", Id.toString(hash.getBytes()));

		// non mmap (<= 16 KB file)
		hash = fileService.calculateFileHash(Path.of(Objects.requireNonNull(getClass().getResource("/upnp/routers/RT-AC87U.xml")).toURI()), ioBuffer);
		assertNotNull(hash);
		assertEquals("a045c2c987b55e6c29082ded01a9abf33ad4cf9d", Id.toString(hash.getBytes()));
	}

	@Test
	void ScanShare_Success() throws URISyntaxException
	{
		var share = ShareFakes.createShare(Path.of(Objects.requireNonNull(getClass().getResource("/image")).toURI()));
		fileService.scanShare(share);
		verify(fileNotificationService).startScanning(share);
		verify(fileNotificationService, times(2)).startScanningFile(any());
		verify(fileNotificationService, times(2)).stopScanningFile();
		verify(fileNotificationService).stopScanning();
	}

	@Test
	void DeleteFile_SingleFile_Success()
	{
		// Root
		var fileRoot = FileFakes.createFile("C:\\", null);

		// Share
		var fileGreatGrandParent = FileFakes.createFile("share", fileRoot);
		var share = ShareFakes.createShare(fileGreatGrandParent);

		var fileGrandParent = FileFakes.createFile("media", fileGreatGrandParent);

		var fileParent = FileFakes.createFile("images", fileGrandParent);

		var file = FileFakes.createFile("foobar.jpg", fileParent);

		// C:\share\media\images\foobar.jpg

		when(fileRepository.countByParent(fileParent)).thenReturn(1);
		when(shareRepository.findShareByFile(fileParent)).thenReturn(Optional.empty());

		when(fileRepository.countByParent(fileGrandParent)).thenReturn(1);
		when(shareRepository.findShareByFile(fileGrandParent)).thenReturn(Optional.empty());

		when(fileRepository.countByParent(fileGreatGrandParent)).thenReturn(1);
		when(shareRepository.findShareByFile(fileGreatGrandParent)).thenReturn(Optional.of(share));

		when(fileRepository.countByParent(fileRoot)).thenReturn(0);
		when(shareRepository.findShareByFile(fileRoot)).thenReturn(Optional.empty());

		fileService.deleteFile(file);

		verify(fileRepository, times(0)).countByParent(file);
		verify(shareRepository, times(0)).findShareByFile(file);

		verify(fileRepository, times(1)).countByParent(fileParent);
		verify(shareRepository, times(1)).findShareByFile(fileParent);

		verify(fileRepository, times(1)).countByParent(fileGrandParent);
		verify(shareRepository, times(1)).findShareByFile(fileGrandParent);

		verify(fileRepository, times(1)).countByParent(fileGreatGrandParent);
		verify(shareRepository, times(1)).findShareByFile(fileGreatGrandParent);

		verify(fileRepository, times(0)).countByParent(fileRoot);
		verify(shareRepository, times(0)).findShareByFile(fileRoot);

		verify(fileRepository, times(0)).delete(file);
		verify(fileRepository, times(1)).delete(fileGrandParent);
	}

	@Test
	void DeleteFile_TwoFiles_Success()
	{
		// Root
		var fileRoot = FileFakes.createFile("C:\\", null);

		// Share
		var fileGreatGrandParent = FileFakes.createFile("share", fileRoot);
		var share = ShareFakes.createShare(fileGreatGrandParent);

		var fileGrandParent = FileFakes.createFile("media", fileGreatGrandParent);

		var fileParent = FileFakes.createFile("images", fileGrandParent);

		var file = FileFakes.createFile("foobar.jpg", fileParent);
		var file2 = FileFakes.createFile("plop.jpg", fileParent);

		// C:\share\media\images\foobar.jpg and plop.jpg

		when(fileRepository.countByParent(fileParent)).thenReturn(2);
		when(shareRepository.findShareByFile(fileParent)).thenReturn(Optional.empty());

		when(fileRepository.countByParent(fileGrandParent)).thenReturn(1);
		when(shareRepository.findShareByFile(fileGrandParent)).thenReturn(Optional.empty());

		fileService.deleteFile(file);

		verify(fileRepository, times(0)).countByParent(file);
		verify(shareRepository, times(0)).findShareByFile(file);

		verify(fileRepository, times(1)).countByParent(fileParent);
		verify(shareRepository, times(0)).findShareByFile(fileParent);

		verify(fileRepository, times(0)).countByParent(fileGrandParent);
		verify(shareRepository, times(0)).findShareByFile(fileGrandParent);

		verify(fileRepository, times(0)).countByParent(fileGreatGrandParent);
		verify(shareRepository, times(0)).findShareByFile(fileGreatGrandParent);

		verify(fileRepository, times(0)).countByParent(fileRoot);
		verify(shareRepository, times(0)).findShareByFile(fileRoot);

		verify(fileRepository, times(1)).delete(file);
		verify(fileRepository, times(0)).delete(fileGrandParent);
	}

	@Test
	void DeleteFile_SingleFileButAnotherUpper_Success()
	{
		// Root
		var fileRoot = FileFakes.createFile("C:\\", null);

		// Share
		var fileGreatGrandParent = FileFakes.createFile("share", fileRoot);
		var share = ShareFakes.createShare(fileGreatGrandParent);

		var fileGrandParent = FileFakes.createFile("media", fileGreatGrandParent);

		var fileParent = FileFakes.createFile("images", fileGrandParent);
		var fileParent2 = FileFakes.createFile("videos", fileGrandParent);

		var file = FileFakes.createFile("foobar.jpg", fileParent);
		var file2 = FileFakes.createFile("plop.avi", fileParent2);

		// C:\share\media\images\foobar.jpg and plop.avi is in media\videos

		when(fileRepository.countByParent(fileParent)).thenReturn(1);
		when(shareRepository.findShareByFile(fileParent)).thenReturn(Optional.empty());

		when(fileRepository.countByParent(fileGrandParent)).thenReturn(2);
		when(shareRepository.findShareByFile(fileGrandParent)).thenReturn(Optional.empty());

		fileService.deleteFile(file);

		verify(fileRepository, times(0)).countByParent(file);
		verify(shareRepository, times(0)).findShareByFile(file);

		verify(fileRepository, times(1)).countByParent(fileParent);
		verify(shareRepository, times(1)).findShareByFile(fileParent);

		verify(fileRepository, times(1)).countByParent(fileGrandParent);
		verify(shareRepository, times(0)).findShareByFile(fileGrandParent);

		verify(fileRepository, times(0)).countByParent(fileGreatGrandParent);
		verify(shareRepository, times(0)).findShareByFile(fileGreatGrandParent);

		verify(fileRepository, times(0)).countByParent(fileRoot);
		verify(shareRepository, times(0)).findShareByFile(fileRoot);

		verify(fileRepository, times(0)).delete(file);
		verify(fileRepository, times(1)).delete(fileParent);
		verify(fileRepository, times(0)).delete(fileGrandParent);
	}

	@Test
	void DeleteFile_SingleFileButNotShare_Success()
	{
		// Root
		var fileRoot = FileFakes.createFile("C:\\", null);

		// Share
		var fileParent = FileFakes.createFile("share", fileRoot);
		var share = ShareFakes.createShare(fileParent);

		var file = FileFakes.createFile("foobar.jpg", fileParent);

		// C:\share\foobar.jpg

		when(fileRepository.countByParent(fileParent)).thenReturn(1);
		when(shareRepository.findShareByFile(fileParent)).thenReturn(Optional.of(share));

		when(fileRepository.countByParent(fileRoot)).thenReturn(1);
		when(shareRepository.findShareByFile(fileRoot)).thenReturn(Optional.empty());

		fileService.deleteFile(file);

		verify(fileRepository, times(0)).countByParent(file);
		verify(shareRepository, times(0)).findShareByFile(file);

		verify(fileRepository, times(1)).countByParent(fileParent);
		verify(shareRepository, times(1)).findShareByFile(fileParent);

		verify(fileRepository, times(0)).countByParent(fileRoot);
		verify(shareRepository, times(0)).findShareByFile(fileRoot);

		verify(fileRepository, times(1)).delete(file);
		verify(fileRepository, times(0)).delete(fileParent);
		verify(fileRepository, times(0)).delete(fileRoot);
	}

}