/*
 * Copyright (c) 2019-2020 by David Gerber - https://zapek.com
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

package io.xeres.common.dto.profile;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import io.xeres.common.dto.location.LocationDTO;
import io.xeres.common.id.ProfileFingerprint;
import io.xeres.common.pgp.Trust;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static io.xeres.common.dto.profile.ProfileConstants.NAME_LENGTH_MAX;
import static io.xeres.common.dto.profile.ProfileConstants.NAME_LENGTH_MIN;

public record ProfileDTO(

		long id,

		@NotNull(message = "Name is mandatory")
		@Size(message = "Name length must be between " + NAME_LENGTH_MIN + " and " + NAME_LENGTH_MAX + " characters", min = NAME_LENGTH_MIN, max = NAME_LENGTH_MAX)
		String name,

		String pgpIdentifier,

		@Size(min = ProfileFingerprint.LENGTH, max = ProfileFingerprint.LENGTH)
		@Schema(example = "nhgF6ITwm/LLqchhpwJ91KFfAxg=")
		byte[] pgpFingerprint,

		byte[] pgpPublicKeyData,

		boolean accepted,

		Trust trust,

		@JsonInclude(NON_EMPTY)
		List<LocationDTO> locations
)
{
	public ProfileDTO
	{
		if (locations == null)
		{
			locations = new ArrayList<>();
		}
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		var that = (ProfileDTO) o;
		return name.equals(that.name) && Objects.equals(pgpIdentifier, that.pgpIdentifier) && Arrays.equals(pgpFingerprint, that.pgpFingerprint) && Arrays.equals(pgpPublicKeyData, that.pgpPublicKeyData);
	}

	@Override
	public int hashCode()
	{
		var result = Objects.hash(name, pgpIdentifier);
		result = 31 * result + Arrays.hashCode(pgpFingerprint);
		result = 31 * result + Arrays.hashCode(pgpPublicKeyData);
		return result;
	}

	@Override
	public String toString()
	{
		return "ProfileDTO{" +
				"name='" + name + '\'' +
				", pgpIdentifier='" + pgpIdentifier + '\'' +
				'}';
	}
}
