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

package io.xeres.common.dto.location;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.xeres.common.dto.connection.ConnectionDTO;
import io.xeres.common.id.LocationId;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

public record LocationDTO(

		long id,

		@NotNull(message = "Name is mandatory")
		@JsonProperty("name")
		String name,

		@NotNull(message = "Location identifier is mandatory")
		@Size(min = LocationId.LENGTH, max = LocationId.LENGTH)
		byte[] locationIdentifier,

		String hostname,

		@JsonInclude(NON_EMPTY)
		List<ConnectionDTO> connections,

		boolean connected,

		Instant lastConnected
)
{
	public LocationDTO
	{
		if (connections == null)
		{
			connections = new ArrayList<>();
		}
	}
}
