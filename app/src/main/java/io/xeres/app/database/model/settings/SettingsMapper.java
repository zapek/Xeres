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

package io.xeres.app.database.model.settings;

import io.xeres.common.dto.settings.SettingsDTO;

public final class SettingsMapper
{
	private SettingsMapper()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static SettingsDTO toDTO(Settings settings)
	{
		if (settings == null)
		{
			return null;
		}

		return new SettingsDTO(
				settings.getTorSocksHost(),
				settings.getTorSocksPort(),
				settings.getI2pSocksHost(),
				settings.getI2pSocksPort(),
				settings.isUpnpEnabled(),
				settings.isBroadcastDiscoveryEnabled(),
				settings.isDhtEnabled(),
				settings.isAutoStartEnabled(),
				settings.getIncomingDirectory(),
				settings.getRemotePassword(),
				settings.isRemoteEnabled(),
				settings.isUpnpRemoteEnabled(),
				settings.getRemotePort()
		);
	}

	public static Settings fromDTO(SettingsDTO dto)
	{
		if (dto == null)
		{
			return null;
		}

		var settings = new Settings();
		settings.setTorSocksHost(dto.torSocksHost());
		settings.setTorSocksPort(dto.torSocksPort());
		settings.setI2pSocksHost(dto.i2pSocksHost());
		settings.setI2pSocksPort(dto.i2pSocksPort());
		settings.setUpnpEnabled(dto.upnpEnabled());
		settings.setBroadcastDiscoveryEnabled(dto.broadcastDiscoveryEnabled());
		settings.setDhtEnabled(dto.dhtEnabled());
		settings.setAutoStartEnabled(dto.autoStartEnabled());
		settings.setIncomingDirectory(dto.incomingDirectory());
		settings.setRemotePassword(dto.remotePassword());
		settings.setRemoteEnabled(dto.remoteEnabled());
		settings.setUpnpRemoteEnabled(dto.upnpRemoteEnabled());
		settings.setRemotePort(dto.remotePort());
		return settings;
	}
}
