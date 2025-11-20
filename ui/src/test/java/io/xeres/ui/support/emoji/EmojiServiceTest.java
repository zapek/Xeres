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

package io.xeres.ui.support.emoji;

import io.xeres.ui.properties.UiClientProperties;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmojiServiceTest
{
	@Mock
	private UiClientProperties uiClientProperties;

	// We cannot use @InjectMocks because EmojiService performs
	// computations that requires mocks in the constructor and that
	// is executed before "when" statements can be done.
	private EmojiService createEmojiService()
	{
		return new EmojiService(uiClientProperties, new JsonMapper());
	}


	@ParameterizedTest
	@CsvSource({
			"\uD83E\uDD71, 1f971",
			"\uD83C\uDDE6\uD83C\uDDE8, 1f1e6-1f1e8",
			"\uD83D\uDEA3\uD83C\uDFFD\u200D\u2640\uFE0F, 1f6a3-1f3fd-200d-2640-fe0f"
	})
	void CodeDecimalToUnicode_Success(String input, String expected)
	{
		var emojiService = createEmojiService();
		var result = emojiService.emojiToFileName(input);

		assertEquals(expected, result);
	}

	@ParameterizedTest
	@CsvSource({
			"hello, hello",
			";-), 😉",
			":wink:, 😉",
			":wink: :wink, 😉 :wink",
			":wink :wink:, :wink 😉"
	})
	void ToUnicode_Success(String input, String expected)
	{
		when(uiClientProperties.isSmileyToUnicode()).thenReturn(true);
		when(uiClientProperties.isRsEmojisAliases()).thenReturn(true);

		var emojiService = createEmojiService();

		var result = emojiService.toUnicode(input);

		assertEquals(expected, result);
	}
}
