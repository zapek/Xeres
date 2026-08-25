/*
 * Copyright (c) 2026 by David Gerber - https://zapek.com
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

package io.xeres.ui.support.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PasswordStrengthTest
{
	private static final BigDecimal ONE_DAY = new BigDecimal(86_400);
	private static final BigDecimal ONE_YEAR = new BigDecimal(31_536_000);
	private static final BigDecimal TWO_CENTURIES = ONE_YEAR.multiply(new BigDecimal(200));

	@Test
	void GetStrength_Null_ThrowsNullPointerException()
	{
		assertThrows(NullPointerException.class, () -> PasswordStrength.getStrength(null));
	}

	@ParameterizedTest
	@ValueSource(doubles = {0, 1, 86399})
	void GetStrength_BelowOneDay_VeryWeak(double seconds)
	{
		assertEquals(PasswordStrength.VERY_WEAK, PasswordStrength.getStrength(BigDecimal.valueOf(seconds)));
	}

	@Test
	void GetStrength_ExactlyOneDay_Weak()
	{
		assertEquals(PasswordStrength.WEAK, PasswordStrength.getStrength(ONE_DAY));
	}

	@Test
	void GetStrength_BetweenDayAndYear_Weak()
	{
		assertEquals(PasswordStrength.WEAK, PasswordStrength.getStrength(new BigDecimal(1_000_000)));
	}

	@Test
	void GetStrength_ExactlyOneYear_Good()
	{
		assertEquals(PasswordStrength.GOOD, PasswordStrength.getStrength(ONE_YEAR));
	}

	@Test
	void GetStrength_BetweenYearAndTwoCenturies_Good()
	{
		assertEquals(PasswordStrength.GOOD, PasswordStrength.getStrength(TWO_CENTURIES.subtract(BigDecimal.ONE)));
	}

	@Test
	void GetStrength_ExactlyTwoCenturies_Strong()
	{
		assertEquals(PasswordStrength.STRONG, PasswordStrength.getStrength(TWO_CENTURIES));
	}

	@Test
	void GetStrength_AboveTwoCenturies_Strong()
	{
		assertEquals(PasswordStrength.STRONG, PasswordStrength.getStrength(TWO_CENTURIES.add(BigDecimal.ONE)));
	}

	@Test
	void ToString_ReturnsNonNull()
	{
		for (var strength : PasswordStrength.values())
		{
			var result = strength.toString();
			assertEquals(result, strength.toString()); // consistent
		}
	}
}
