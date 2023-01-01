/*
 * Copyright (c) 2019-2023 by David Gerber - https://zapek.com
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

package io.xeres.app.net.peer.bootstrap;

import io.xeres.app.database.DatabaseSessionManager;
import io.xeres.app.net.peer.PeerConnectionManager;
import io.xeres.app.properties.NetworkProperties;
import io.xeres.app.service.LocationService;
import io.xeres.app.service.SettingsService;
import io.xeres.app.xrs.service.serviceinfo.ServiceInfoRsService;
import io.xeres.ui.support.tray.TrayService;
import org.springframework.stereotype.Component;

@Component
public class PeerTcpServer extends PeerServer
{
	public PeerTcpServer(SettingsService settingsService, NetworkProperties networkProperties, LocationService locationService, PeerConnectionManager peerConnectionManager, DatabaseSessionManager databaseSessionManager, ServiceInfoRsService serviceInfoRsService, TrayService trayService)
	{
		super(settingsService, networkProperties, locationService, peerConnectionManager, databaseSessionManager, serviceInfoRsService, trayService);
	}
}
