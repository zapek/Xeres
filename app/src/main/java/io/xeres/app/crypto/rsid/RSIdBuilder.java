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

import io.xeres.app.database.model.connection.Connection;
import io.xeres.app.database.model.profile.Profile;
import io.xeres.app.net.protocol.PeerAddress;
import io.xeres.common.id.LocationId;
import io.xeres.common.rsid.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RSIdBuilder
{
	private final Type type;
	private byte[] name;
	private LocationId locationId;
	private Profile profile;
	private byte[] pgpFingerprint;
	private final List<String> locators = new ArrayList<>();
	private String externalLocator;
	private String lanLocator;
	private String dnsLocator;

	public RSIdBuilder(Type type)
	{
		this.type = type;
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

	public RSIdBuilder setProfile(Profile profile)
	{
		this.profile = profile;
		return this;
	}

	public RSIdBuilder setPgpFingerprint(byte[] pgpFingerprint)
	{
		this.pgpFingerprint = pgpFingerprint;
		return this;
	}

	public RSIdBuilder addLocator(Connection connection)
	{
		if (externalLocator == null && connection.isExternal())
		{
			externalLocator = connection.getAddress();
		}
		else if (lanLocator == null && !connection.isExternal())
		{
			lanLocator = connection.getAddress();
		}
		else if (dnsLocator == null && connection.getType() == PeerAddress.Type.HOSTNAME)
		{
			dnsLocator = connection.getAddress();
		}
		else
		{
			locators.add(connection.getType().scheme() + connection.getAddress());
		}
		return this;
	}

	public RSId build()
	{
		RSId rsId = switch (type)
				{
					case SHORT_INVITE -> {
						var si = new ShortInvite();

						Objects.requireNonNull(name);
						Objects.requireNonNull(locationId);
						Objects.requireNonNull(pgpFingerprint);

						si.setName(name);
						si.setLocationId(locationId);
						si.setPgpFingerprint(pgpFingerprint);

						if (externalLocator != null)
						{
							si.setExt4Locator(externalLocator);
						}
						if (lanLocator != null)
						{
							si.setLoc4Locator(lanLocator);
						}
						if (dnsLocator != null)
						{
							si.setDnsName(dnsLocator);
						}
						locators.forEach(si::addLocator);

						yield si;
					}
					case CERTIFICATE -> {
						var cert = new RSCertificate();

						Objects.requireNonNull(name);
						Objects.requireNonNull(locationId);
						Objects.requireNonNull(profile);

						cert.setName(name);
						cert.setLocationId(locationId);
						cert.setVerifiedPgpPublicKey(profile.getPgpPublicKeyData());

						if (externalLocator != null)
						{
							cert.setExternalIp(externalLocator);
						}
						if (lanLocator != null)
						{
							cert.setInternalIp(lanLocator);
						}
						if (dnsLocator != null)
						{
							cert.setDnsName(dnsLocator);
						}
						locators.forEach(cert::addLocator);

						yield cert;
					}
					default -> null;
				};

		if (rsId != null)
		{
			rsId.checkRequiredFields();
		}
		return rsId;
	}
}
