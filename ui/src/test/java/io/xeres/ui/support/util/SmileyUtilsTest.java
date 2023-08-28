/*
 * Copyright (c) 2019-2023 by David Gerber - https://zapek.com
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

import io.xeres.testutils.TestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SmileyUtilsTest
{
	@ParameterizedTest
	@CsvSource({
			":-),\uD83D\uDE42",
			"hello :-),hello \uD83D\uDE42"
	})
	void SmileyUtils_SmileysToUnicode_Replace(String input, String expected)
	{
		var actualValue = SmileyUtils.smileysToUnicode(input);
		assertEquals(expected, actualValue);
	}

	@Test
	void SmileyUtils_NoInstance_OK() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(SmileyUtils.class);
	}

	@Test
	void SmileyUtils_SmileysToUnicode_NoReplace()
	{
		var value = SmileyUtils.smileysToUnicode("hello:-)");
		assertEquals("hello:-)", value);
	}
}
