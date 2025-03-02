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

	void onGxsTunnelDataReceived(Location tunnelId, byte[] data);

	boolean onGxsTunnelDataAuthorization(GxsId sender, Location tunnelId, boolean clientSide);

	void onGxsTunnelStatusChanged(Location tunnelId, GxsTunnelStatus status);
}
