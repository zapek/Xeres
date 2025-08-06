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

package io.xeres.ui.support.markdown;

import io.xeres.ui.FXTest;
import io.xeres.ui.custom.DisclosedHyperlink;
import io.xeres.ui.support.contentline.*;
import io.xeres.ui.support.emoji.EmojiService;
import io.xeres.ui.support.markdown.MarkdownService.ParsingMode;
import javafx.scene.text.Text;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MarkdownServiceTest extends FXTest
{
	@Mock
	private EmojiService emojiService;

	// We cannot use @InjectMocks because MarkdownService performs
	// computations that requires mocks in the constructor and that
	// is executed before "when" statements can be done.
	private MarkdownService createMarkdownService()
	{
		return new MarkdownService(emojiService);
	}

	@BeforeAll
	void configureMock()
	{
		when(emojiService.isColoredEmojis()).thenReturn(true);
		when(emojiService.toUnicode(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	void Parse_Sanitize_Default_Success()
	{
		var markdownService = createMarkdownService();

		var text = """
				Line1



				Line2 with trails  \s

				Line3
				Line4
				
				
				Line 5
				""";

		var wanted = """
				Line1

				Line2 with trails

				Line3
				Line4
				
				Line 5""";

		assertEquals(wanted, markdownService.parse(text, Set.of(), null).stream()
				.map(Content::asText)
				.collect(Collectors.joining()));
	}

	@Test
	void Parse_Sanitize_Default_Verbatim_Success()
	{
		var markdownService = createMarkdownService();

		var text = """
				Line1
				> Line2
				> Line3
				""";

		var wanted = """
				Line1
				
				> Line2
				> Line3""";

		assertEquals(wanted, markdownService.parse(text, EnumSet.of(ParsingMode.PARAGRAPH), null).stream()
				.map(Content::asText)
				.collect(Collectors.joining()));
	}

	@Test
	void Parse_Sanitize_Quoted_Success()
	{
		var markdownService = createMarkdownService();

		var text = """
				> Line1
				> Line2
				
				Line3
				""";

		var wanted = """
				> Line1
				> Line2
				
				Line3""";

		assertEquals(wanted, markdownService.parse(text, EnumSet.of(ParsingMode.PARAGRAPH), null).stream()
				.map(Content::asText)
				.collect(Collectors.joining()));
	}

	@Test
	void Sanitize_NoEndOfLine_Success()
	{
		var markdownService = createMarkdownService();

		var text = """
				Line1



				Line2 with trails  \s

				Line3
				Line4
				""";

		var wanted = """
				Line1
				
				Line2 with trails
				
				Line3
				Line4""";

		assertEquals(wanted, markdownService.parse(text, Set.of(), null).stream()
				.map(Content::asText)
				.collect(Collectors.joining()));
	}

	@Test
	void Sanitize_Paragraph_Success()
	{
		var markdownService = createMarkdownService();

		var text = """
				Line1



				Line2 with trails  \s

				Line3
				Line4
				""";

		var wanted = """
				Line1

				Line2 with trails

				Line3 Line4""";

		var result = markdownService.parse(text, EnumSet.of(ParsingMode.PARAGRAPH), null).stream()
				.map(Content::asText)
				.collect(Collectors.joining());

		assertEquals(wanted, result);
	}

	@Test
	void ParseInlineUrls_Success()
	{
		var markdownService = createMarkdownService();

		var input = "Hello world! https://xeres.io is the site to visit now!";

		var output = markdownService.parse(input, Set.of(), null);

		assertEquals(3, output.size());
		assertInstanceOf(ContentText.class, output.get(0));
		assertInstanceOf(ContentUri.class, output.get(1));
		assertInstanceOf(ContentText.class, output.get(2));

		assertEquals("Hello world! ", ((Text) output.get(0).getNode()).getText());
		assertEquals("https://xeres.io", ((DisclosedHyperlink) output.get(1).getNode()).getText());
		assertEquals(" is the site to visit now!", ((Text) output.get(2).getNode()).getText());
	}

	@Test
	void ParseInlineUrls_WeirdChars_Success()
	{
		var markdownService = createMarkdownService();

		var input = "https://www.foobar.com/watch?v=aXfS2p_ZyHY";

		var output = markdownService.parse(input, Set.of(), null);

		assertEquals(1, output.size());
		assertInstanceOf(ContentUri.class, output.getFirst());

		assertEquals(input, ((DisclosedHyperlink) output.getFirst().getNode()).getText());
	}

	@ParameterizedTest
	@CsvSource({
			"    foo();, foo();",
			"\tfoo();, foo();",
			"        foo();,     foo();"
	})
	void RemoveFirstStartingSpacesCode(String input, String expected)
	{
		var markdownService = createMarkdownService();

		var output = markdownService.parse(input, Set.of(), null);

		assertEquals(expected, ((Text) output.getFirst().getNode()).getText());
	}

	@Test
	void Parse_Empty()
	{
		var markdownService = createMarkdownService();

		var input = "\n";

		var output = markdownService.parse(input, Set.of(), null);

		assertEquals(0, output.size());
	}

	@Test
	void Parse_Empty_Too()
	{
		var markdownService = createMarkdownService();

		var input = "\n\n";

		var output = markdownService.parse(input, Set.of(), null);

		assertEquals(0, output.size());
	}

	@Test
	void Parse_Simple_Text()
	{
		var markdownService = createMarkdownService();

		var input = "hello, world\n";

		var output = markdownService.parse(input, Set.of(), null);

		assertEquals(1, output.size());

		assertInstanceOf(ContentText.class, output.getFirst());
		assertEquals("hello, world", ((Text) output.getFirst().getNode()).getText());
	}

	@Test
	void Parse_OneLine_Several()
	{
		var markdownService = createMarkdownService();

		var input = "https://zapek.com !\n";

		var output = markdownService.parse(input, Set.of(), null);

		assertEquals(2, output.size());

		assertInstanceOf(ContentUri.class, output.get(0));
		assertEquals("https://zapek.com", ((DisclosedHyperlink) output.get(0).getNode()).getText());

		assertInstanceOf(ContentText.class, output.get(1));
		assertEquals(" !", ((Text) output.get(1).getNode()).getText());
	}

	@Test
	void Parse_Multiline_Several()
	{
		var markdownService = createMarkdownService();

		var line1 = "https://zapek.com :-) **yeah**\n";
		var line2 = "and another one: `fork();` it is\n";
		var input = line1 + line2;

		var output = markdownService.parse(input, Set.of(), null);

		assertEquals(7, output.size());

		assertInstanceOf(ContentUri.class, output.get(0));
		assertEquals("https://zapek.com", ((DisclosedHyperlink) output.get(0).getNode()).getText());

		assertInstanceOf(ContentText.class, output.get(1));
		assertEquals(" :-) ", ((Text) output.get(1).getNode()).getText());

		assertInstanceOf(ContentEmphasis.class, output.get(2));
		assertEquals("yeah", ((Text) output.get(2).getNode()).getText());
		assertEquals("-fx-font-weight: bold;", output.get(2).getNode().getStyle());

		assertInstanceOf(ContentText.class, output.get(3));
		assertEquals("\n", ((Text) output.get(3).getNode()).getText());

		assertInstanceOf(ContentText.class, output.get(4));
		assertEquals("and another one: ", ((Text) output.get(4).getNode()).getText());

		assertInstanceOf(ContentCode.class, output.get(5));
		assertEquals("fork();", ((Text) output.get(5).getNode()).getText());

		assertInstanceOf(ContentText.class, output.get(6));
		assertEquals(" it is", ((Text) output.get(6).getNode()).getText());
	}
}
