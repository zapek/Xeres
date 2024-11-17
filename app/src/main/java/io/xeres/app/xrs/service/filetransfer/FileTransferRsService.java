/*
 * Copyright (c) 2024 by David Gerber - https://zapek.com
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

package io.xeres.app.xrs.service.filetransfer;

import io.xeres.app.crypto.rscrypto.RsCrypto;
import io.xeres.app.database.DatabaseSession;
import io.xeres.app.database.DatabaseSessionManager;
import io.xeres.app.database.model.location.Location;
import io.xeres.app.database.repository.FileDownloadRepository;
import io.xeres.app.net.peer.PeerConnection;
import io.xeres.app.net.peer.PeerConnectionManager;
import io.xeres.app.properties.NetworkProperties;
import io.xeres.app.service.LocationService;
import io.xeres.app.service.SettingsService;
import io.xeres.app.service.file.FileService;
import io.xeres.app.service.notification.file.FileSearchNotificationService;
import io.xeres.app.service.notification.file.FileTrendNotificationService;
import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.item.ItemUtils;
import io.xeres.app.xrs.service.RsService;
import io.xeres.app.xrs.service.RsServiceInitPriority;
import io.xeres.app.xrs.service.RsServiceRegistry;
import io.xeres.app.xrs.service.RsServiceType;
import io.xeres.app.xrs.service.filetransfer.item.*;
import io.xeres.app.xrs.service.turtle.TurtleRouter;
import io.xeres.app.xrs.service.turtle.TurtleRsClient;
import io.xeres.app.xrs.service.turtle.item.*;
import io.xeres.common.id.LocationId;
import io.xeres.common.id.Sha1Sum;
import io.xeres.common.rest.file.FileProgress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import static io.xeres.app.properties.NetworkProperties.*;
import static io.xeres.app.xrs.service.RsServiceType.FILE_TRANSFER;
import static io.xeres.app.xrs.service.RsServiceType.TURTLE;

@Component
public class FileTransferRsService extends RsService implements TurtleRsClient
{
	private static final Logger log = LoggerFactory.getLogger(FileTransferRsService.class);
	private TurtleRouter turtleRouter;

	static final int CHUNK_SIZE = 1024 * 1024; // 1 MB
	static final int BLOCK_SIZE = 1024 * 8; // 8 KB

	private final FileService fileService;
	private final PeerConnectionManager peerConnectionManager;
	private final FileSearchNotificationService fileSearchNotificationService;
	private final FileTrendNotificationService fileTrendNotificationService;
	private final RsServiceRegistry rsServiceRegistry;
	private final DatabaseSessionManager databaseSessionManager;
	private final LocationService locationService;
	private final SettingsService settingsService;
	private final RsCrypto.EncryptionFormat encryptionFormat;
	private final FileTransferStrategy fileTransferStrategy;
	private final FileDownloadRepository fileDownloadRepository;
	private FileTransferManager fileTransferManager;
	private Thread fileTransferManagerThread;

	private final BlockingQueue<Action> fileCommandQueue = new LinkedBlockingQueue<>();

	private Location ownLocation;

	private final Map<Sha1Sum, Sha1Sum> encryptedHashes = new ConcurrentHashMap<>();

	public FileTransferRsService(RsServiceRegistry rsServiceRegistry, FileService fileService, PeerConnectionManager peerConnectionManager, FileSearchNotificationService fileSearchNotificationService, FileTrendNotificationService fileTrendNotificationService, DatabaseSessionManager databaseSessionManager, LocationService locationService, SettingsService settingsService, NetworkProperties networkProperties, FileDownloadRepository fileDownloadRepository)
	{
		super(rsServiceRegistry);
		this.fileService = fileService;
		this.peerConnectionManager = peerConnectionManager;
		this.fileSearchNotificationService = fileSearchNotificationService;
		this.rsServiceRegistry = rsServiceRegistry;
		this.fileTrendNotificationService = fileTrendNotificationService;
		this.databaseSessionManager = databaseSessionManager;
		this.locationService = locationService;
		this.settingsService = settingsService;
		encryptionFormat = getEncryptionFormat(networkProperties);
		fileTransferStrategy = getFileTransferStrategy(networkProperties);
		this.fileDownloadRepository = fileDownloadRepository;
	}

	private static RsCrypto.EncryptionFormat getEncryptionFormat(NetworkProperties networkProperties)
	{
		if (networkProperties.getTunnelEncryption().equals(TUNNEL_ENCRYPTION_CHACHA20_SHA256))
		{
			return RsCrypto.EncryptionFormat.CHACHA20_SHA256;
		}
		else if (networkProperties.getTunnelEncryption().equals(TUNNEL_ENCRYPTION_CHACHA20_POLY1305))
		{
			return RsCrypto.EncryptionFormat.CHACHA20_POLY1305;
		}
		else
		{
			throw new IllegalArgumentException("Unsupported encryption format: " + networkProperties.getTunnelEncryption());
		}
	}

	private static FileTransferStrategy getFileTransferStrategy(NetworkProperties networkProperties)
	{
		if (networkProperties.getFileTransferStrategy().equals(FILE_TRANSFER_STRATEGY_LINEAR))
		{
			return FileTransferStrategy.LINEAR;
		}
		else if (networkProperties.getFileTransferStrategy().equals(FILE_TRANSFER_STRATEGY_RANDOM))
		{
			return FileTransferStrategy.RANDOM;
		}
		else
		{
			throw new IllegalArgumentException("Unsupported file transfer strategy: " + networkProperties.getFileTransferStrategy());
		}
	}

	@Override
	public void initialize()
	{
		try (var ignored = new DatabaseSession(databaseSessionManager))
		{
			ownLocation = locationService.findOwnLocation().orElseThrow();
			fileDownloadRepository.deleteAllByCompletedTrue();
			fileDownloadRepository.findAllByLocationIsNull()
					.forEach(file -> fileCommandQueue.add(new ActionDownload(file.getId(), file.getName(), file.getHash(), file.getSize(), null, file.getChunkMap())));
		}

		fileTransferManager = new FileTransferManager(this, fileService, settingsService, locationService, databaseSessionManager, ownLocation, fileCommandQueue, fileTransferStrategy);

		fileTransferManagerThread = Thread.ofVirtual()
				.name("File Transfer Manager")
				.start(fileTransferManager);
	}

	@Override
	public RsServiceType getServiceType()
	{
		return FILE_TRANSFER;
	}

	@Override
	public RsServiceType getMasterServiceType()
	{
		return TURTLE;
	}

	@Override
	public RsServiceInitPriority getInitPriority()
	{
		return RsServiceInitPriority.NORMAL;
	}

	@Override
	public void initializeTurtle(TurtleRouter turtleRouter)
	{
		this.turtleRouter = turtleRouter;
	}

	@Override
	public void initialize(PeerConnection peerConnection)
	{
		try (var ignored = new DatabaseSession(databaseSessionManager))
		{
			fileDownloadRepository.findAllByLocation(peerConnection.getLocation())
					.forEach(file -> fileCommandQueue.add(new ActionDownload(file.getId(), file.getName(), file.getHash(), file.getSize(), file.getLocation().getLocationId(), file.getChunkMap())));
		}
	}

	@Override
	public void handleItem(PeerConnection sender, Item item)
	{
		switch (item)
		{
			case FileTransferDataRequestItem ftItem -> // XXX: check for upload limit for this peer and drop it if exceeded!
					fileCommandQueue.add(new ActionReceiveDataRequest(sender.getLocation(), ftItem.getFileItem().hash(), ftItem.getFileOffset(), ftItem.getChunkSize()));
			case FileTransferDataItem ftItem -> fileCommandQueue.add(new ActionReceiveData(sender.getLocation(), ftItem.getFileData().fileItem().hash(), ftItem.getFileData().offset(), ftItem.getFileData().data()));

			case FileTransferChunkMapRequestItem ftItem -> fileCommandQueue.add(new ActionReceiveChunkMapRequest(sender.getLocation(), ftItem.getHash(), ftItem.isLeecher()));
			case FileTransferChunkMapItem ftItem -> fileCommandQueue.add(new ActionReceiveChunkMap(sender.getLocation(), ftItem.getHash(), ftItem.getCompressedChunks()));

			case FileTransferSingleChunkCrcRequestItem ftItem -> fileCommandQueue.add(new ActionReceiveSingleChunkCrcRequest(sender.getLocation(), ftItem.getHash(), ftItem.getChunkNumber()));
			case FileTransferSingleChunkCrcItem ftItem -> fileCommandQueue.add(new ActionReceiveSingleChunkCrc(sender.getLocation(), ftItem.getHash(), ftItem.getChunkNumber(), ftItem.getCheckSum()));
			default -> log.debug("Unhandled item {}", item);
		}
	}

	@Override
	public boolean handleTunnelRequest(PeerConnection sender, Sha1Sum hash)
	{
		// - find file by encrypted hash (and get its real hash)
		// - the correspondence can be put in the encryptedHashes, because the tunnel will likely be established
		var file = fileService.findFileByEncryptedHash(hash);
		if (file.isPresent())
		{
			log.debug("Found file {}", file.get());
			var path = fileService.getFilePath(file.get());
			if (!Files.isRegularFile(path))
			{
				log.debug("File {} doesn't exist on disk, not serving it and removing", file.get());
				fileService.deleteFile(file.get());
				return false;
			}

			// Add it to the encrypted hashes because it's going to be used soon
			// to establish the tunnels
			encryptedHashes.put(hash, file.get().getHash());

			// XXX: don't forget to handle files currently being swarmed and tons of other things
			// XXX: sender might not necessarily be needed (it's for the permissions)
			return true;
		}
		return false;
	}

	@Override
	public void receiveTurtleData(TurtleGenericTunnelItem item, Sha1Sum hash, Location virtualLocation, TunnelDirection tunnelDirection)
	{
		switch (item)
		{
			case TurtleGenericDataItem turtleGenericDataItem ->
			{
				var realHash = encryptedHashes.get(hash);
				if (realHash == null)
				{
					log.error("Cannot find the real hash of hash {}", hash);
					return;
				}

				var decryptedItem = decryptItem(turtleGenericDataItem, realHash);
				if (decryptedItem instanceof TurtleGenericDataItem)
				{
					log.error("Decrypted item is a recursive bomb, dropping");
				}
				else
				{
					receiveTurtleData(decryptedItem, realHash, virtualLocation, tunnelDirection);
				}
				// No need to dispose decryptedItem as it doesn't come from netty
			}

			case TurtleFileRequestItem turtleFileRequestItem -> fileCommandQueue.add(new ActionReceiveDataRequest(virtualLocation, hash, turtleFileRequestItem.getChunkOffset(), turtleFileRequestItem.getChunkSize()));
			case TurtleFileDataItem turtleFileDataItem -> fileCommandQueue.add(new ActionReceiveData(virtualLocation, hash, turtleFileDataItem.getChunkOffset(), turtleFileDataItem.getChunkData()));

			case TurtleFileMapRequestItem turtleFileMapRequestItem -> fileCommandQueue.add(new ActionReceiveChunkMapRequest(virtualLocation, hash, turtleFileMapRequestItem.getDirection() == TunnelDirection.CLIENT));
			case TurtleFileMapItem turtleFileMapItem -> fileCommandQueue.add(new ActionReceiveChunkMap(virtualLocation, hash, turtleFileMapItem.getCompressedChunks()));

			case TurtleChunkCrcRequestItem turtleChunkCrcRequestItem -> fileCommandQueue.add(new ActionReceiveSingleChunkCrcRequest(virtualLocation, hash, turtleChunkCrcRequestItem.getChunkNumber()));
			case TurtleChunkCrcItem turtleChunkCrcItem -> fileCommandQueue.add(new ActionReceiveSingleChunkCrc(virtualLocation, hash, turtleChunkCrcItem.getChunkNumber(), turtleChunkCrcItem.getChecksum()));

			case null -> throw new IllegalStateException("Null item");
			default -> log.warn("Unknown packet type received: {}", item.getSubType());
		}
	}

	@Override
	public List<byte[]> receiveSearchRequest(byte[] query, int maxHits)
	{
		return List.of();
	}

	@Override
	public void receiveSearchRequestString(String keywords)
	{
		fileTrendNotificationService.receivedSearch(keywords);
	}

	@Override
	public void receiveSearchResult(int requestId, TurtleSearchResultItem item)
	{
		if (item instanceof TurtleFileSearchResultItem fileItem)
		{
			log.debug("Forwarding search result id {} as notification", requestId);
			fileItem.getResults().forEach(fileInfo -> fileSearchNotificationService.foundFile(requestId, fileInfo.getFileName(), fileInfo.getFileSize(), fileInfo.getFileHash()));
		}
	}

	@Override
	public void addVirtualPeer(Sha1Sum encryptedHash, Location virtualLocation, TunnelDirection direction)
	{
		var hash = encryptedHashes.get(encryptedHash);
		if (hash == null)
		{
			log.warn("Couldn't add virtual peer, not an encrypted hash");
			return;
		}
		if (direction == TunnelDirection.SERVER)
		{
			fileCommandQueue.add(new ActionAddPeer(hash, virtualLocation));
		}
	}

	@Override
	public void removeVirtualPeer(Sha1Sum encryptedHash, Location virtualLocation)
	{
		var hash = encryptedHashes.get(encryptedHash);
		if (hash == null)
		{
			log.warn("Couldn't remove virtual peer, not an encrypted hash");
			return;
		}
		fileCommandQueue.add(new ActionRemovePeer(hash, virtualLocation));
	}

	public int turtleSearch(String search) // XXX: maybe make a generic version or so...
	{
		if (turtleRouter != null) // Happens if the service is not enabled
		{
			return turtleRouter.turtleSearch(search, this);
		}
		return 0;
	}

	@Transactional
	public long download(String name, Sha1Sum hash, long size, LocationId locationId)
	{
		var id = fileService.addDownload(name, hash, size, locationService.findLocationByLocationId(locationId).orElse(null));
		if (id != 0L)
		{
			fileCommandQueue.add(new ActionDownload(id, name, hash, size, locationId, null));
		}
		return id;
	}

	public void markDownloadAsCompleted(Sha1Sum hash)
	{
		fileService.markDownloadAsCompleted(hash);
	}

	public List<FileProgress> getDownloadStatistics()
	{
		fileCommandQueue.add(new ActionGetDownloadsProgress());
		return fileTransferManager.getDownloadsProgress();
	}

	public List<FileProgress> getUploadStatistics()
	{
		fileCommandQueue.add(new ActionGetUploadsProgress());
		return fileTransferManager.getUploadsProgress();
	}

	public void removeDownload(long id)
	{
		fileCommandQueue.add(new ActionRemoveDownload(id));
	}

	@Override
	public void shutdown()
	{
		fileSearchNotificationService.shutdown();
		fileTrendNotificationService.shutdown();
		if (fileTransferManagerThread != null)
		{
			log.info("Stopping FileTransferManager...");
			fileTransferManagerThread.interrupt();
			try
			{
				log.info("Waiting for FileTransferManager to terminate...");
				fileTransferManagerThread.join();
				log.debug("FileTransferManager terminated");
			}
			catch (InterruptedException e)
			{
				log.error("Failed to wait for termination: {}", e.getMessage(), e);
				Thread.currentThread().interrupt();
			}
		}
	}

	private void sendTurtleItem(Location virtualLocation, Sha1Sum hash, TurtleGenericTunnelItem item)
	{
		// We only send encrypted tunnels. They're available since Retroshare 0.6.2
		turtleRouter.sendTurtleData(virtualLocation, encryptItem(item, hash));
	}

	private TurtleGenericDataItem encryptItem(TurtleGenericTunnelItem item, Sha1Sum hash)
	{
		var key = new FileTransferEncryptionKey(hash);
		var serializedItem = ItemUtils.serializeItem(item, this);
		return new TurtleGenericDataItem(RsCrypto.encryptAuthenticateData(key, serializedItem, encryptionFormat));
	}

	private TurtleGenericTunnelItem decryptItem(TurtleGenericDataItem item, Sha1Sum hash)
	{
		var key = new FileTransferEncryptionKey(hash);
		return (TurtleGenericTunnelItem) ItemUtils.deserializeItem(RsCrypto.decryptAuthenticateData(key, item.getTunnelData()), rsServiceRegistry);
	}

	public void activateTunnels(Sha1Sum hash)
	{
		var encryptedHash = FileService.encryptHash(hash);
		encryptedHashes.put(encryptedHash, hash);

		turtleRouter.startMonitoringTunnels(encryptedHash, this, true);
	}

	public void deactivateTunnels(Sha1Sum hash)
	{
		var encryptedHash = FileService.encryptHash(hash);
		encryptedHashes.put(encryptedHash, hash);

		turtleRouter.stopMonitoringTunnels(encryptedHash);
	}

	/**
	 * Sends request as a client.
	 *
	 * @param location  the location to send to (can be virtual)
	 * @param hash      the hash related to
	 * @param size      the size
	 * @param offset    the offset
	 * @param chunkSize the chunk size (usually 1 MB)
	 */
	public void sendDataRequest(Location location, Sha1Sum hash, long size, long offset, int chunkSize)
	{
		if (turtleRouter.isVirtualPeer(location))
		{
			var item = new TurtleFileRequestItem(offset, chunkSize);
			sendTurtleItem(location, hash, item);
		}
		else
		{
			var item = new FileTransferDataRequestItem(size, hash, offset, chunkSize);
			peerConnectionManager.writeItem(location, item, this);
		}
	}

	/**
	 * Sends a chunk map request.
	 *
	 * @param location the location to send to (can be virtual)
	 * @param hash     the hash related to
	 * @param isClient if true, means that the message is for a client (that is, one that is currently downloading the file) instead of a server
	 */
	public void sendChunkMapRequest(Location location, Sha1Sum hash, boolean isClient)
	{
		if (turtleRouter.isVirtualPeer(location))
		{
			var item = new TurtleFileMapRequestItem();
			sendTurtleItem(location, hash, item);
		}
		else
		{
			var item = new FileTransferChunkMapRequestItem(hash, isClient);
			peerConnectionManager.writeItem(location, item, this);
		}
	}

	void sendChunkMap(Location location, Sha1Sum hash, boolean isClient, List<Integer> compressedChunkMap)
	{
		if (turtleRouter.isVirtualPeer(location))
		{
			var item = new TurtleFileMapItem(compressedChunkMap);
			sendTurtleItem(location, hash, item);
		}
		else
		{
			var item = new FileTransferChunkMapItem(hash, compressedChunkMap, isClient);
			peerConnectionManager.writeItem(location, item, this);
		}
	}

	public void sendSingleChunkCrcRequest(Location location, Sha1Sum hash, int chunkNumber)
	{
		if (turtleRouter.isVirtualPeer(location))
		{
			var item = new TurtleChunkCrcRequestItem(chunkNumber);
			sendTurtleItem(location, hash, item);
		}
		else
		{
			var item = new FileTransferSingleChunkCrcRequestItem(hash, chunkNumber);
			peerConnectionManager.writeItem(location, item, this);
		}
	}

	public void sendSingleChunkCrc(Location location, Sha1Sum hash, int chunkNumber, Sha1Sum checkSum)
	{
		if (turtleRouter.isVirtualPeer(location))
		{
			var item = new TurtleChunkCrcItem(chunkNumber, checkSum);
			sendTurtleItem(location, hash, item);
		}
		else
		{
			var item = new FileTransferSingleChunkCrcItem(hash, chunkNumber, checkSum);
			peerConnectionManager.writeItem(location, item, this);
		}
	}

	/**
	 * Sends data as a server.
	 *
	 * @param location  the location to send to (can be virtual too)
	 * @param hash      the hash related to it
	 * @param totalSize the total size of the file
	 * @param offset    the offset within the file
	 * @param data      the data to send
	 */
	void sendData(Location location, Sha1Sum hash, long totalSize, long offset, byte[] data)
	{
		if (data.length > 0)
		{
			if (data.length > BLOCK_SIZE)
			{
				throw new IllegalArgumentException("Maximum send totalSize must be " + BLOCK_SIZE + ", not " + data.length);
			}

			if (turtleRouter.isVirtualPeer(location))
			{
				var item = new TurtleFileDataItem(offset, data);
				sendTurtleItem(location, hash, item);
			}
			else
			{
				var item = new FileTransferDataItem(offset, totalSize, hash, data);
				peerConnectionManager.writeItem(location, item, this);
			}
		}
		else
		{
			log.debug("Empty data, nothing to send. Bug?!");
		}
	}
}
