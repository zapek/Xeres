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

package io.xeres.common.dto.settings;

import io.xeres.testutils.BooleanFakes;
import io.xeres.testutils.IdFakes;
import org.apache.commons.lang3.RandomStringUtils;

public final class SettingsDTOFakes
{
	private SettingsDTOFakes()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static SettingsDTO create()
	{
		return new SettingsDTO(RandomStringUtils.secure().nextAlphanumeric(30),
				IdFakes.createInt(),
				RandomStringUtils.secure().nextAlphanumeric(30),
				IdFakes.createInt(),
				BooleanFakes.create(),
				BooleanFakes.create(),
				BooleanFakes.create(),
				BooleanFakes.create(),
				"/foo/bar",
				"foobar1234",
				BooleanFakes.create(),
				BooleanFakes.create());
	}
}
