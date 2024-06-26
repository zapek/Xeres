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

import io.xeres.app.database.model.location.Location;
import io.xeres.common.id.Sha1Sum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

class FileTransferAgent
{
	private static final Logger log = LoggerFactory.getLogger(FileTransferAgent.class);

	private final FileTransferRsService fileTransferRsService;
	private final FileProvider fileProvider;
	private Sha1Sum hash;

	private List<Location> peers = new ArrayList<>();

	public FileTransferAgent(FileTransferRsService fileTransferRsService, Sha1Sum hash, FileProvider fileProvider)
	{
		this.fileTransferRsService = fileTransferRsService;
		this.hash = hash;
		this.fileProvider = fileProvider;
	}

	public FileProvider getFileProvider()
	{
		return fileProvider;
	}

	public void addPeer(Location peer)
	{
		peers.add(peer);
	}

	void removePeer(Location peer)
	{
		if (!peers.remove(peer))
		{
			log.warn("Removal of peer {} failed because it's not in the list. This shouldn't happen.", peer);
		}
	}

	void askForNextParts()
	{
		if (fileProvider instanceof FileLeecher)
		{
			// File being downloaded

			if (peers.isEmpty())
			{
				log.warn("Asked for next parts even tough there are no peers. Shouldn't happen.");
				return;
			}
			var peer = peers.getFirst();

			fileTransferRsService.sendDataRequest(peer, hash, fileProvider.getFileSize(), 0, FileTransferRsService.CHUNK_SIZE); // XXX: fix! as it only works with files < 1MB...
		}
		else if (fileProvider instanceof FileSeeder)
		{
			// File being served
			log.error("Not implemented yet");
		}
	}
}