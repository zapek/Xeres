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

package io.xeres.app.service;

import io.xeres.app.application.autostart.AutoStart;
import io.xeres.common.rest.config.Capabilities;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class CapabilityService
{
	private final AutoStart autoStart;

	public CapabilityService(AutoStart autoStart)
	{
		this.autoStart = autoStart;
	}

	public Set<String> getCapabilities()
	{
		Set<String> capabilities = new HashSet<>();

		if (autoStart.isSupported())
		{
			capabilities.add(Capabilities.AUTOSTART);
		}
		return capabilities;
	}
}
