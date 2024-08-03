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
import io.xeres.app.xrs.service.turtle.item.TurtleGenericTunnelItem;
import io.xeres.common.id.Sha1Sum;

/**
 * Represents a Turtle Router. It is given to Turtle Clients in the initialization method to enable its functions to be called anytime.
 * <p>
 * Only encrypted hashes are supported.
 */
public interface TurtleRouter
{
	/**
	 * Starts monitoring tunnels for a given hash.
	 * <p>
	 * Should be called before downloading a file so that the turtle router can provide the tunnels for it.
	 *
	 * @param hash              the encrypted hash to monitor tunnels for
	 * @param client            the {@link TurtleRsClient}
	 * @param allowMultiTunnels true to allow multiple tunnels to be created (aggressive mode), otherwise only use one tunnel
	 */
	void startMonitoringTunnels(Sha1Sum hash, TurtleRsClient client, boolean allowMultiTunnels);

	/**
	 * Stops monitoring tunnels for a given hash.
	 * <p>
	 * Should be called after a download is finished (successfully or not) so that the tunnels can be cleaned up.
	 *
	 * @param hash the encrypted hash to stops monitoring tunnels for
	 */
	void stopMonitoringTunnels(Sha1Sum hash);

	/**
	 * Forces to re-digg a tunnel.
	 *
	 * @param hash the encrypted hash to re-digg a tunnel for
	 */
	void forceReDiggTunnel(Sha1Sum hash);

	/**
	 * Sends data using Turtle.
	 *
	 * @param virtualPeer the virtual peer to send data to
	 * @param item        the data represented by any subclass of {@link TurtleGenericTunnelItem}
	 */
	void sendTurtleData(Location virtualPeer, TurtleGenericTunnelItem item);

	/**
	 * Checks if a location is a virtual turtle peer.
	 *
	 * @param location the location
	 * @return true if it's a virtual turtle peer
	 */
	boolean isVirtualPeer(Location location);

	/**
	 * Performs a tunnel search.
	 *
	 * @param search the search string
	 * @param client a {@link TurtleRsClient}
	 * @return the search id
	 */
	int turtleSearch(String search, TurtleRsClient client);
}
