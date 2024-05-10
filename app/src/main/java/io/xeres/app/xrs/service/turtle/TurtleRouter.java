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

import io.xeres.app.xrs.service.turtle.item.TurtleGenericTunnelItem;
import io.xeres.common.id.LocationId;
import io.xeres.common.id.Sha1Sum;

public interface TurtleRouter
{
	void startMonitoringTunnels(Sha1Sum hash, TurtleRsClient client, boolean allowMultiTunnels); // XXX: better name?

	void stopMonitoringTunnels(Sha1Sum hash);

	/**
	 * Forces to re-digg a tunnel.
	 *
	 * @param hash the hash to re-digg a tunnel for
	 */
	void forceReDiggTunnel(Sha1Sum hash);

	void sendTurtleData(LocationId virtualPeerId, TurtleGenericTunnelItem item);

	boolean isVirtualPeer(LocationId locationId);
}