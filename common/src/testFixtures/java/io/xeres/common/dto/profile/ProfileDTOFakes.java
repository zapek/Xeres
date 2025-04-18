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

package io.xeres.common.dto.profile;

import io.xeres.common.dto.location.LocationDTOFakes;
import io.xeres.common.pgp.Trust;
import io.xeres.testutils.BooleanFakes;
import io.xeres.testutils.EnumFakes;
import io.xeres.testutils.IdFakes;
import io.xeres.testutils.StringFakes;

import java.time.Instant;
import java.util.List;

public final class ProfileDTOFakes
{
	private ProfileDTOFakes()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static ProfileDTO create()
	{
		return new ProfileDTO(IdFakes.createLong(), StringFakes.createNickname(), Long.toString(IdFakes.createLong()), Instant.now(), new byte[20], new byte[1], BooleanFakes.create(), EnumFakes.create(Trust.class), List.of(LocationDTOFakes.create()));
	}
}
