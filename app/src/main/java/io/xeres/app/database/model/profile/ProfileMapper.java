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

package io.xeres.app.database.model.profile;

import io.xeres.app.database.model.location.LocationMapper;
import io.xeres.common.dto.profile.ProfileDTO;
import io.xeres.common.id.ProfileFingerprint;

import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;

public final class ProfileMapper
{
	private ProfileMapper()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static ProfileDTO toDTO(Profile profile)
	{
		if (profile == null)
		{
			return null;
		}

		return new ProfileDTO(
				profile.getId(),
				profile.getName(),
				Long.toString(profile.getPgpIdentifier()),
				profile.getProfileFingerprint().getBytes(),
				profile.getPgpPublicKeyData(),
				profile.isAccepted(),
				profile.getTrust(),
				new ArrayList<>());
	}

	public static ProfileDTO toDeepDTO(Profile profile)
	{
		if (profile == null)
		{
			return null;
		}
		var profileDTO = toDTO(profile);

		profileDTO.locations().addAll(profile.getLocations().stream()
				.map(LocationMapper::toDeepDTO)
				.toList());

		return profileDTO;
	}

	public static List<ProfileDTO> toDTOs(List<Profile> profiles)
	{
		return emptyIfNull(profiles).stream()
				.map(ProfileMapper::toDTO)
				.toList();
	}

	public static List<ProfileDTO> toDeepDTOs(List<Profile> profiles)
	{
		return emptyIfNull(profiles).stream()
				.map(ProfileMapper::toDeepDTO)
				.toList();
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
		profile.setProfileFingerprint(new ProfileFingerprint(dto.pgpFingerprint()));
		profile.setPgpPublicKeyData(dto.pgpPublicKeyData());
		profile.setAccepted(dto.accepted());
		profile.setTrust(dto.trust());
		return profile;
	}
}
