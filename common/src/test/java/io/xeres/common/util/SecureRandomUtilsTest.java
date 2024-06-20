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

package io.xeres.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecureRandomUtilsTest
{
	@Test
	void SecureRandomUtils_NextPassword_Empty()
	{
		char[] password = new char[0];

		assertThrows(IllegalArgumentException.class, () -> SecureRandomUtils.nextPassword(password));
	}

	@Test
	void SecureRandomUtils_NextPassword_Short()
	{
		char[] password = new char[1];
		SecureRandomUtils.nextPassword(password);

		assertTrue(String.valueOf(password).chars().anyMatch(Character::isDigit));
	}

	@Test
	void SecureRandomUtils_NextPassword_Small()
	{
		char[] password = new char[2];
		SecureRandomUtils.nextPassword(password);

		assertTrue(String.valueOf(password).chars().anyMatch(Character::isDigit));
	}

	@Test
	void SecureRandomUtils_NextPassword_Minimal()
	{
		char[] password = new char[3];
		SecureRandomUtils.nextPassword(password);

		assertTrue(String.valueOf(password).chars().anyMatch(Character::isDigit));
		assertTrue(String.valueOf(password).chars().anyMatch(Character::isLowerCase));
		assertTrue(String.valueOf(password).chars().anyMatch(Character::isUpperCase));
	}

	@Test
	void SecureRandomUtils_NextPassword_Normal()
	{
		var password = new char[20];
		SecureRandomUtils.nextPassword(password);

		assertTrue(String.valueOf(password).chars().anyMatch(Character::isDigit));
		assertTrue(String.valueOf(password).chars().anyMatch(Character::isLowerCase));
		assertTrue(String.valueOf(password).chars().anyMatch(Character::isUpperCase));
	}
}