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

import io.xeres.common.dto.profile.ProfileDTO;
import io.xeres.common.id.ProfileFingerprint;
import io.xeres.ui.model.location.LocationMapper;

@SuppressWarnings("DuplicatedCode")
public final class ProfileMapper
{
	private ProfileMapper()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static Profile fromDTO(ProfileDTO dto)
	{
		if (dto == null)
		{
			return null;
		}

		var profile = new Profile();
		profile.setId(dto.id());
		profile.setName(dto.name());
		profile.setPgpIdentifier(Long.parseLong(dto.pgpIdentifier()));
		profile.setCreated(dto.created());
		profile.setProfileFingerprint(new ProfileFingerprint(dto.pgpFingerprint()));
		profile.setPgpPublicKeyData(dto.pgpPublicKeyData());
		profile.setAccepted(dto.accepted());
		profile.setTrust(dto.trust());
		return profile;
	}

	public static Profile fromDeepDTO(ProfileDTO dto)
	{
		if (dto == null)
		{
			return null;
		}

		var profile = fromDTO(dto);

		profile.getLocations().addAll(dto.locations().stream()
				.map(LocationMapper::fromDeepDTO)
				.toList());

		return profile;
	}
}
