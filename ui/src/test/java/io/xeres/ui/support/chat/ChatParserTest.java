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

package io.xeres.ui.support.chat;

import io.xeres.ui.support.contentline.ContentText;
import javafx.scene.text.Text;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class ChatParserTest
{
	@Test
	void ChatParser_ParseActionMe_OK()
	{
		var nickname = "foobar";
		var input = "/me is hungry";

		var result = ChatParser.parseActionMe(input, nickname);

		assertEquals(nickname + " is hungry", result);
	}

	@Test
	void ChatParser_IsActionMe_OK()
	{
		assertTrue(ChatParser.isActionMe("/me is happy"));
		assertFalse(ChatParser.isActionMe("and wants to use Xeres"));
	}

	@ParameterizedTest
	@CsvSource({
			"hello world,hello world",
			";-),\uD83D\uDE09",
			":wink:,\uD83D\uDE09"
	})
	void ChatParser_Parse_Text(String input, String expected)
	{
		var result = ChatParser.parse(input);

		assertEquals(1, result.size());
		assertInstanceOf(ContentText.class, result.get(0));
		assertEquals(expected, ((Text) result.get(0).getNode()).getText());
	}

	@Test
	void ChatParser_Parse_Href_Fail()
	{
		// This is an illegal input. A non retroshare:// URL which is not supported. The result are empty texts.
		var input = "<a href=\"http://foobar.com\">super site</a>";
		var result = ChatParser.parse(input);

		assertEquals(2, result.size());
		assertInstanceOf(ContentText.class, result.get(0));
		assertEquals("", ((Text) result.get(0).getNode()).getText());
	}

	// Some tests cannot be done because JavaFX is not available fully
}
