/*
 * Copyright (c) 2025 by David Gerber - https://zapek.com
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

package io.xeres.app.xrs.service.gxstunnel;

import io.xeres.app.database.model.location.Location;
import io.xeres.app.xrs.service.RsServiceSlave;
import io.xeres.common.id.GxsId;

public interface GxsTunnelRsClient extends RsServiceSlave
{
	/**
	 * Called to initialize the gxs tunnel client.
	 *
	 * @param gxsTunnelRsService the {@link GxsTunnelRsService}. Is used to call service methods.
	 * @return the service number
	 */
	int onGxsTunnelInitialization(GxsTunnelRsService gxsTunnelRsService);

	/**
	 * Called when data is received from the tunnel.
	 *
	 * @param tunnelId the tunnel id
	 * @param data     the data
	 */
	void onGxsTunnelDataReceived(Location tunnelId, byte[] data);

	/**
	 * Called when a remote is requesting to establish a tunnel.
	 *
	 * @param sender the sender of the request
	 * @param tunnelId the tunnel id
	 * @param clientSide true if it's a client tunnel, false means it's a server tunnel
	 * @return true if the tunnel is accepted
	 */
	boolean onGxsTunnelDataAuthorization(GxsId sender, Location tunnelId, boolean clientSide);

	/**
	 * Called when the tunnel status changes.
	 *
	 * @param tunnelId the tunnel id
	 * @param destination the destination of the tunnel
	 * @param status the new status
	 */
	void onGxsTunnelStatusChanged(Location tunnelId, GxsId destination, GxsTunnelStatus status);
}
