/*
 * Copyright (c) 2024 by David Gerber - https://zapek.com
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

package io.xeres.common.dto.share;

import io.xeres.common.pgp.Trust;
import io.xeres.testutils.BooleanFakes;
import io.xeres.testutils.EnumFakes;
import io.xeres.testutils.IdFakes;
import io.xeres.testutils.StringFakes;

import java.time.Instant;

public final class ShareDTOFakes
{
	private ShareDTOFakes()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static ShareDTO createShareDTO()
	{
		return new ShareDTO(IdFakes.createLong(), StringFakes.createNickname(), "C:\\foobar", BooleanFakes.create(), EnumFakes.create(Trust.class), Instant.now());
	}
}
