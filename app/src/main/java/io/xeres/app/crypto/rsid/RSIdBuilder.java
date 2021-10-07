/*
 * Copyright (c) 2019-2021 by David Gerber - https://zapek.com
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

package io.xeres.app.crypto.rsid;

import io.xeres.app.crypto.rsid.shortinvite.ShortInvite;
import io.xeres.common.id.LocationId;

import java.util.ArrayList;
import java.util.List;

public class RSIdBuilder
{
	private byte[] name;
	private LocationId locationId;
	private byte[] pgpFingerprint;
	private final List<String> locators = new ArrayList<>();
	private String externalLocator;

	public RSIdBuilder(RSId.Type type)
	{
	}

	public RSIdBuilder setName(byte[] name)
	{
		this.name = name;
		return this;
	}

	public RSIdBuilder setLocationId(LocationId locationId)
	{
		this.locationId = locationId;
		return this;
	}

	public RSIdBuilder setPgpFingerprint(byte[] pgpFingerprint)
	{
		this.pgpFingerprint = pgpFingerprint;
		return this;
	}

	public RSIdBuilder addLocator(String address, boolean external)
	{
		// We ignore the internal address on purpose as we expect broadcast discovery to work
		if (external)
		{
			if (externalLocator == null)
			{
				externalLocator = address;
			}
			else
			{
				locators.add("ipv4://" + address); // XXX
			}
		}
		return this;
	}

	public RSId build()
	{
		var si = new ShortInvite();

		si.setName(name);
		si.setLocationId(locationId);
		si.setPgpFingerprint(pgpFingerprint);
		if (externalLocator != null)
		{
			si.setExt4Locator(externalLocator);
		}
		locators.stream().findFirst().ifPresent(si::setLoc4Locator);
		locators.stream().skip(1).forEach(si::addLocator);

		return si;
	}
}
