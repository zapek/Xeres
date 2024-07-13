package io.xeres.app.xrs.service.filetransfer;

import io.xeres.app.database.model.location.Location;
import io.xeres.common.id.Sha1Sum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static io.xeres.app.xrs.service.filetransfer.FileTransferRsService.BLOCK_SIZE;

/**
 * Responsible for sending a chunk to a remote location.
 */
class ChunkSender
{
	private static final Logger log = LoggerFactory.getLogger(ChunkSender.class);

	private final FileTransferRsService fileTransferRsService;
	private final Location location;
	private final FileProvider provider;
	private final Sha1Sum hash;
	private final long totalSize;
	private long offset;
	private int size;

	public ChunkSender(FileTransferRsService fileTransferRsService, Location location, FileProvider provider, Sha1Sum hash, long totalSize, long offset, int size)
	{
		this.fileTransferRsService = fileTransferRsService;
		this.location = location;
		this.provider = provider;
		this.hash = hash;
		this.totalSize = totalSize;
		this.offset = offset;
		this.size = size;
	}

	public boolean send()
	{
		var length = Math.min(BLOCK_SIZE, size);

		byte[] data;
		try
		{
			data = provider.read(offset, length);
		}
		catch (IOException e)
		{
			log.error("Failed to read file", e);
			return false;
		}
		fileTransferRsService.sendData(location, hash, totalSize, offset, data);

		size -= length;
		offset += length;

		return size > 0;
	}
}
