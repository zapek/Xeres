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

package io.xeres.common.dto.location;

import io.xeres.common.dto.connection.ConnectionDTOFakes;
import io.xeres.common.location.Availability;
import io.xeres.testutils.BooleanFakes;
import io.xeres.testutils.IdFakes;
import io.xeres.testutils.StringFakes;
import io.xeres.testutils.TimeFakes;

import java.util.List;

public final class LocationDTOFakes
{
	private LocationDTOFakes()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static LocationDTO create()
	{
		return new LocationDTO(IdFakes.createLong(), StringFakes.createNickname(), IdFakes.createLocationIdentifier().getBytes(), StringFakes.createNickname(), List.of(ConnectionDTOFakes.createConnectionDTO()), BooleanFakes.create(), TimeFakes.createInstant(), Availability.AVAILABLE, "Xeres 2.3.2");
	}
}
