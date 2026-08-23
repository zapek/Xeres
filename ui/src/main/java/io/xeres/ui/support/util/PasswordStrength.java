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

import io.xeres.common.i18n.I18nEnum;
import io.xeres.common.i18n.I18nUtils;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.ResourceBundle;

/// Evaluates a password strength depending on the time it would take to crack
/// it theoretically by using brute force.
enum PasswordStrength implements I18nEnum
{
	VERY_WEAK,
	WEAK,
	GOOD,
	STRONG;

	private final ResourceBundle bundle = I18nUtils.getBundle();

	private static final BigDecimal ONE_DAY = new BigDecimal(86_400);
	private static final BigDecimal ONE_YEAR = new BigDecimal(31_536_000);
	private static final BigDecimal TWO_CENTURIES = ONE_YEAR.multiply(new BigDecimal(200));

	@Override
	public String toString()
	{
		return bundle.getString(getMessageKey(this));
	}

	public static PasswordStrength getStrength(BigDecimal secondsToCrack)
	{
		Objects.requireNonNull(secondsToCrack);

		if (secondsToCrack.compareTo(ONE_DAY) < 0)
		{
			return VERY_WEAK;
		}
		else if (secondsToCrack.compareTo(ONE_YEAR) < 0)
		{
			return WEAK;
		}
		else if (secondsToCrack.compareTo(TWO_CENTURIES) < 0)
		{
			return GOOD;
		}
		else
		{
			return STRONG;
		}
	}
}
