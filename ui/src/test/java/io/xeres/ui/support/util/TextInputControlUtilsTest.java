/*
 * Copyright (c) 2025 by David Gerber - https://zapek.com
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextInputControlUtilsTest
{
	@ParameterizedTest
	@ValueSource(strings = {
			"hey",
			"hello ;) how are you? ;)",
			" "
	})
	void pasteGuessedContent_Code_False(String input)
	{
		assertFalse(TextInputControlUtils.isSourceCode(input));
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"""
					class TextInputControlUtilsTest {
					    @ParameterizedTest
					    @ValueSource(strings = {
					        "hey",
					        "hello ;) how are you? ;)",
					        "simple text",
					        "another example"
					    })
					    void pasteGuessedContent_False(String input) {
					        assertFalse(TextInputControlUtils.isSourceCode(input));
					    }
					}
					""",
			"""
					function calculateTotal(items) {
					  return items.reduce((sum, item) => sum + item.price, 0);
					}
					""",
			"""
						function calculateTotal(items) {
						  return items.reduce((sum, item) => sum + item.price, 0);
						}
					""",
			"""
					try {
					  const response = await apiCall();
					  setData(response.data);
					} catch (error) {
					  console.error('Failed:', error);
					}
					""",
			"""
					.container {
					  display: flex;
					  justify-content: center;
					  align-items: center;
					}
					""",
			"""
					<div class="header">
					  <h1>Welcome</h1>
					</div>
					""",
			"""
					for (let i = 0; i < array.length; i++) {
					  process(array[i]);
					}
					""",
			"""
					{
					  "user": {
					    "id": 123,
					    "name": "Alice"
					  }
					}
					""",
			"""
					let counter: number = 0;
					const MAX_RETRIES: number = 3;
					"""
	})
	void pasteGuessedContent_Code_True(String input)
	{
		assertTrue(TextInputControlUtils.isSourceCode(input));
	}

	@Test
	void pasteGuessedContent_Citation_False()
	{
		assertFalse(TextInputControlUtils.isCitation("this is just some text"));
	}

	@Test
	void pasteGuessedContent_Citation_True()
	{
		assertTrue(TextInputControlUtils.isCitation("Flying Pigs Reported That a Cow Managed to Stop Global Warming by Eating Using Only One Stomach"));
	}

	@Test
	void pasteGuessedContent_Uri_False()
	{
		assertFalse(TextInputControlUtils.isUri("not an url"));
	}

	@Test
	void pasteGuessedContent_Uri_True()
	{
		assertTrue(TextInputControlUtils.isUri("https://example.com"));
		assertTrue(TextInputControlUtils.isUri("http://example.com"));
		assertTrue(TextInputControlUtils.isUri("retroshare://foo"));
	}
}