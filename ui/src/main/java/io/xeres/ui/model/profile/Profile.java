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

package io.xeres.ui.model.profile;

import io.xeres.common.id.ProfileFingerprint;
import io.xeres.common.pgp.Trust;
import io.xeres.ui.model.location.Location;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Profile
{
	private long id;
	private String name;
	private long pgpIdentifier;
	private Instant created;
	private ProfileFingerprint profileFingerprint;
	private byte[] pgpPublicKeyData;
	private boolean accepted;
	private Trust trust;
	private final List<Location> locations = new ArrayList<>();

	public long getId()
	{
		return id;
	}

	public void setId(long id)
	{
		this.id = id;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public long getPgpIdentifier()
	{
		return pgpIdentifier;
	}

	public void setPgpIdentifier(long pgpIdentifier)
	{
		this.pgpIdentifier = pgpIdentifier;
	}

	public Instant getCreated()
	{
		return created;
	}

	public void setCreated(Instant created)
	{
		this.created = created;
	}

	public ProfileFingerprint getProfileFingerprint()
	{
		return profileFingerprint;
	}

	public void setProfileFingerprint(ProfileFingerprint profileFingerprint)
	{
		this.profileFingerprint = profileFingerprint;
	}

	public byte[] getPgpPublicKeyData()
	{
		return pgpPublicKeyData;
	}

	public void setPgpPublicKeyData(byte[] pgpPublicKeyData)
	{
		this.pgpPublicKeyData = pgpPublicKeyData;
	}

	public boolean isAccepted()
	{
		return accepted;
	}

	public void setAccepted(boolean accepted)
	{
		this.accepted = accepted;
	}

	public Trust getTrust()
	{
		return trust;
	}

	public void setTrust(Trust trust)
	{
		this.trust = trust;
	}

	public List<Location> getLocations()
	{
		return locations;
	}
}
