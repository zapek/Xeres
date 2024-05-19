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

import io.xeres.app.net.peer.PeerConnection;
import io.xeres.app.xrs.service.RsServiceSlave;
import io.xeres.app.xrs.service.turtle.item.TunnelDirection;
import io.xeres.app.xrs.service.turtle.item.TurtleGenericTunnelItem;
import io.xeres.app.xrs.service.turtle.item.TurtleSearchResultItem;
import io.xeres.common.id.LocationId;
import io.xeres.common.id.Sha1Sum;

import java.util.List;

public interface TurtleRsClient extends RsServiceSlave
{
	void initializeTurtle(TurtleRouter turtleRouter);
	/**
	 * Asks if this hash can be handled. It usually boils down to searching it in some database or list.
	 * @param sender the peer where it comes from
	 * @param hash the hash
	 * @return true if it can be handled, false otherwise
	 */
	boolean handleTunnelRequest(PeerConnection sender, Sha1Sum hash);

	void receiveTurtleData(TurtleGenericTunnelItem item, Sha1Sum hashOfHash, LocationId virtualLocationId, TunnelDirection tunnelDirection); // XXX: missing turtle generic data item

	/**
	 * Asks to search something.
	 *
	 * @param query   the search query
	 * @param maxHits the maximum number of hits to send back
	 * @return the search results
	 */
	List<byte[]> receiveSearchRequest(byte[] query, int maxHits); // XXX: return a list of results (TurtleFileInfoV2.. actually it's generic stuff so service dependent)

	/**
	 * Informs that a search result has been received.
	 *
	 * @param requestId  the request id
	 * @param searchData the search result
	 */
	void receiveSearchResult(int requestId, TurtleSearchResultItem item);

	void addVirtualPeer(Sha1Sum hash, LocationId virtualLocationId, TunnelDirection direction);

	void removeVirtualPeer(Sha1Sum hash, LocationId virtualLocationId);
}
