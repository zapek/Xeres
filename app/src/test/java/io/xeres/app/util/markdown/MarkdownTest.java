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

package io.xeres.app.util.markdown;

import org.junit.jupiter.api.Test;

import static io.xeres.app.util.markdown.Markdown.HeaderSize.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MarkdownTest
{
	@Test
	void Markdown_AddParagraph_OK()
	{
		var md = new Markdown();
		md.addParagraph("The lazy dog is sleeping");
		md.addParagraph("And the cat too");

		assertEquals("The lazy dog is sleeping\n\nAnd the cat too\n", md.toString());
	}

	@Test
	void Markdown_AddText_OK()
	{
		var md = new Markdown();
		md.addText("hello");
		md.addText("world");

		assertEquals("hello world\n", md.toString());
	}

	@Test
	void Markdown_AddTextWithBreakline_OK()
	{
		var md = new Markdown();
		md.addText("hello");
		md.breakLine();
		md.addText("world");

		assertEquals("hello\nworld\n", md.toString());
	}

	@Test
	void Markdown_AddTextWithDoubleBreakline_OK()
	{
		var md = new Markdown();
		md.addText("hello");
		md.breakLine();
		md.breakLine();
		md.addText("world");

		assertEquals("hello\n\nworld\n", md.toString());
	}

	@Test
	void Markdown_AddHeaders_OK()
	{
		var md = new Markdown();
		md.addHeader("Header 1", H1);
		md.addHeader("Header 2", H2);
		md.addHeader("Header 3", H3);
		md.addHeader("Header 4", H4);
		md.addHeader("Header 5", H5);
		md.addHeader("Header 6", H6);

		assertEquals("""
				# Header 1

				## Header 2

				### Header 3

				#### Header 4

				##### Header 5

				###### Header 6
				""", md.toString());
	}

	@Test
	void Markdown_AddListItems_OK()
	{
		var md = new Markdown();
		md.addListItem("foo");
		md.addListItem("bar");

		assertEquals("""
				    - foo
				    - bar
				""", md.toString());
	}
}
