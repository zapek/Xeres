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

import io.xeres.app.net.peer.PeerConnection;
import io.xeres.app.service.file.FileService;
import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.service.RsService;
import io.xeres.app.xrs.service.RsServiceRegistry;
import io.xeres.app.xrs.service.RsServiceType;
import io.xeres.app.xrs.service.turtle.TurtleRouter;
import io.xeres.app.xrs.service.turtle.TurtleRsClient;
import io.xeres.app.xrs.service.turtle.item.TunnelDirection;
import io.xeres.common.id.LocationId;
import io.xeres.common.id.Sha1Sum;
import org.springframework.stereotype.Component;

import java.util.List;

import static io.xeres.app.xrs.service.RsServiceType.FILE_TRANSFER;
import static io.xeres.app.xrs.service.RsServiceType.TURTLE;

@Component
public class FileTransferRsService extends RsService implements TurtleRsClient
{
	private TurtleRouter turtleRouter;

	private final FileService fileService;

	public FileTransferRsService(RsServiceRegistry rsServiceRegistry, FileService fileService)
	{
		super(rsServiceRegistry);
		this.fileService = fileService;
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
	public void initializeTurtle(TurtleRouter turtleRouter)
	{
		this.turtleRouter = turtleRouter;
	}

	@Override
	public void handleItem(PeerConnection sender, Item item)
	{
		// XXX
	}

	@Override
	public boolean handleTunnelRequest(PeerConnection sender, Sha1Sum hash)
	{
		var file = fileService.findFile(hash);
		if (file.isPresent())
		{
			// XXX: don't forget to handle encrypted hashes, files currently being swarmed and tons of other things
			// XXX: sender might not necessarily be needed (it's for the permissions)
			return true;
		}
		return false;
	}

	@Override
	public void receiveTurtleData(Sha1Sum hash, LocationId virtualLocationId, TunnelDirection tunnelDirection)
	{

	}

	@Override
	public List<byte[]> receiveSearchRequest(byte[] query, int maxHits)
	{
		return List.of();
	}

	@Override
	public void receiveSearchResult(int requestId, byte[] searchData)
	{

	}

	@Override
	public void addVirtualPeer(Sha1Sum hash, LocationId virtualLocationId, TunnelDirection direction)
	{

	}

	@Override
	public void removeVirtualPeer(Sha1Sum hash, LocationId virtualLocationId)
	{

	}
}
