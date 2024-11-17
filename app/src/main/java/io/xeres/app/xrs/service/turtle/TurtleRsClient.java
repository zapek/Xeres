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

package io.xeres.app.xrs.service.turtle;

import io.xeres.app.database.model.location.Location;
import io.xeres.app.net.peer.PeerConnection;
import io.xeres.app.xrs.service.RsServiceSlave;
import io.xeres.app.xrs.service.filetransfer.FileTransferRsService;
import io.xeres.app.xrs.service.turtle.item.TunnelDirection;
import io.xeres.app.xrs.service.turtle.item.TurtleGenericTunnelItem;
import io.xeres.app.xrs.service.turtle.item.TurtleSearchResultItem;
import io.xeres.common.id.Sha1Sum;

import java.util.List;

/**
 * Represents a turtle clients. For example the {@link FileTransferRsService file transfer service} is a turtle client and
 * will receive events from the {@link TurtleRsService turtle router}.
 */
public interface TurtleRsClient extends RsServiceSlave
{
	/**
	 * Called to initialize the turtle client.
	 *
	 * @param turtleRouter  the {@link TurtleRouter}. Keep it somewhere so that you can call its methods.
	 */
	void initializeTurtle(TurtleRouter turtleRouter);

	/**
	 * Called to ask if this hash can be handled.
	 * <p>
	 * It usually boils down to searching it in some database or list.
	 *
	 * @param sender  the {@link PeerConnection} where it comes from
	 * @param hash  the encrypted hash
	 * @return true if it can be handled
	 */
	boolean handleTunnelRequest(PeerConnection sender, Sha1Sum hash);

	/**
	 * Called when receiving data from a tunnel.
	 *
	 * @param item            a {@link TurtleGenericTunnelItem} subclass
	 * @param hash            the encrypted hash from which the data is related to
	 * @param virtualLocation the virtual location
	 * @param tunnelDirection if data is from a {@link TunnelDirection#SERVER} or a {@link TunnelDirection#CLIENT}
	 */
	void receiveTurtleData(TurtleGenericTunnelItem item, Sha1Sum hash, Location virtualLocation, TunnelDirection tunnelDirection);

	/**
	 * Called to ask to search for something.
	 *
	 * @param query  the search query
	 * @param maxHits  the maximum number of hits to send back
	 * @return the search results
	 */
	List<byte[]> receiveSearchRequest(byte[] query, int maxHits); // XXX: return a list of results (TurtleFileInfoV2.. actually it's generic stuff so service dependent)

	void receiveSearchRequestString(String keywords); // XXX: experimental for now...

	/**
	 * Called when receiving search results.
	 *
	 * @param requestId  the request id the search result belongs to
	 * @param item  a {@link TurtleSearchResultItem} subclass containing the results
	 */
	void receiveSearchResult(int requestId, TurtleSearchResultItem item);

	// XXX: document that only encrypted hashes are supported

	/**
	 * Called when a virtual peer related to a hash is added.
	 *
	 * @param hash  the encrypted hash
	 * @param virtualLocation  the virtual location to add
	 * @param direction  the direction of the tunnel, either {@link TunnelDirection#SERVER} or {@link TunnelDirection#CLIENT}
	 */
	void addVirtualPeer(Sha1Sum hash, Location virtualLocation, TunnelDirection direction);

	/**
	 * Called when a virtual peer related to a hash is removed.
	 *
	 * @param hash  the encrypted hash
	 * @param virtualLocation  the virtual location to remove
	 */
	void removeVirtualPeer(Sha1Sum hash, Location virtualLocation);
}
