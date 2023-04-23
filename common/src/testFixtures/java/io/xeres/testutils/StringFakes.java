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

package io.xeres.testutils;

import org.apache.commons.lang3.RandomStringUtils;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public final class StringFakes
{
	private static final String[] FIRSTNAME = {
			"Jean",
			"Alexander",
			"Fernando",
			"Rubens",
			"Valtteri",
			"Jenson",
			"Zhou",
			"Lewis",
			"Robert",
			"Charles",
			"Kevin",
			"Felipe",
			"Nikita",
			"Lando",
			"Esteban",
			"Sergio",
			"Nelson",
			"Alain",
			"George",
			"Carlos",
			"Michael",
			"Ayrton",
			"Lance",
			"Jarno",
			"Yuki",
			"Max",
			"Sebastian"
	};

	private static final String[] LASTNAME = {
			"Alesi",
			"Albon",
			"Alonso",
			"Barrichello",
			"Bottas",
			"Button",
			"Guanyu",
			"Hamilton",
			"Kubica",
			"Leclerc",
			"Magnussen",
			"Massa",
			"Mazepin",
			"Norris",
			"Ocon",
			"Perez",
			"Piquet",
			"Prost",
			"Russell",
			"Sainz",
			"Schumacher",
			"Senna",
			"Stroll",
			"Trulli",
			"Tsunoda",
			"Verstappen",
			"Vettel"
	};

	private StringFakes()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static String createNickname()
	{
		var s = RandomStringUtils.randomAlphabetic(5, 10);
		return s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
	}

	public static String createFirstName()
	{
		return FIRSTNAME[ThreadLocalRandom.current().nextInt(FIRSTNAME.length)];
	}

	public static String createLastName()
	{
		return LASTNAME[ThreadLocalRandom.current().nextInt(LASTNAME.length)];
	}

	public static String createFullName()
	{
		return createFirstName() + " " + createLastName();
	}
}
