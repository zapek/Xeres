/*
 * Copyright (c) 2023 by David Gerber - https://zapek.com
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

package io.xeres.ui.model.settings;

import io.xeres.common.dto.settings.SettingsDTOFakes;
import io.xeres.testutils.TestUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SettingsMapperTest
{
	@Test
	void SettingsMapper_NoInstance_OK() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(SettingsMapper.class);
	}

	@Test
	void SettingsMapper_fromDTO_OK()
	{
		var dto = SettingsDTOFakes.create();

		var settings = SettingsMapper.fromDTO(dto);

		assertEquals(dto.torSocksHost(), settings.getTorSocksHost());
		assertEquals(dto.torSocksPort(), settings.getTorSocksPort());
		assertEquals(dto.i2pSocksHost(), settings.getI2pSocksHost());
		assertEquals(dto.i2pSocksPort(), settings.getI2pSocksPort());
		assertEquals(dto.dhtEnabled(), settings.isDhtEnabled());
		assertEquals(dto.upnpEnabled(), settings.isUpnpEnabled());
		assertEquals(dto.autoStartEnabled(), settings.isAutoStartEnabled());
		assertEquals(dto.broadcastDiscoveryEnabled(), settings.isBroadcastDiscoveryEnabled());
	}
}
