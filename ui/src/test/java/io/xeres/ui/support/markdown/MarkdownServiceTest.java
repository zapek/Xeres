/*
 * Copyright (c) 2025-2026 by David Gerber - https://zapek.com
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
import io.xeres.ui.support.markdown.MarkdownService.Rendering;
import javafx.scene.text.Text;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.EnumSet;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MarkdownServiceTest extends FXTest
{
	private final EmojiService emojiService = mock(EmojiService.class);

	// We cannot use @InjectMocks because MarkdownService performs
	// computations that requires mocks in the constructor and that
	// is executed before "when" statements can be done.
	private MarkdownService createMarkdownService()
	{
		return new MarkdownService(emojiService, null);
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

		assertEquals(wanted, markdownService.parse(text, EnumSet.noneOf(Rendering.class), null).stream()
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

		assertEquals(wanted, markdownService.parse(text, EnumSet.of(Rendering.TEXT_REFLOW), null).stream()
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

		assertEquals(wanted, markdownService.parse(text, EnumSet.of(Rendering.TEXT_REFLOW), null).stream()
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

		assertEquals(wanted, markdownService.parse(text, EnumSet.noneOf(Rendering.class), null).stream()
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

		var result = markdownService.parse(text, EnumSet.of(Rendering.TEXT_REFLOW), null).stream()
				.map(Content::asText)
				.collect(Collectors.joining());

		assertEquals(wanted, result);
	}

	@Test
	void ParseInlineUrls_Success()
	{
		var markdownService = createMarkdownService();

		var input = "Hello world! https://xeres.io is the site to visit now!";

		var output = markdownService.parse(input, EnumSet.noneOf(Rendering.class), null);

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

		var output = markdownService.parse(input, EnumSet.noneOf(Rendering.class), null);

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

		var output = markdownService.parse(input, EnumSet.noneOf(Rendering.class), null);

		assertEquals(expected, ((Text) output.getFirst().getNode()).getText());
	}

	@Test
	void Parse_Empty()
	{
		var markdownService = createMarkdownService();

		var input = "\n";

		var output = markdownService.parse(input, EnumSet.noneOf(Rendering.class), null);

		assertEquals(0, output.size());
	}

	@Test
	void Parse_Empty_Too()
	{
		var markdownService = createMarkdownService();

		var input = "\n\n";

		var output = markdownService.parse(input, EnumSet.noneOf(Rendering.class), null);

		assertEquals(0, output.size());
	}

	@Test
	void Parse_Simple_Text()
	{
		var markdownService = createMarkdownService();

		var input = "hello, world\n";

		var output = markdownService.parse(input, EnumSet.noneOf(Rendering.class), null);

		assertEquals(1, output.size());

		assertInstanceOf(ContentText.class, output.getFirst());
		assertEquals("hello, world", ((Text) output.getFirst().getNode()).getText());
	}

	@Test
	void Parse_OneLine_Several()
	{
		var markdownService = createMarkdownService();

		var input = "https://zapek.com !\n";

		var output = markdownService.parse(input, EnumSet.noneOf(Rendering.class), null);

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

		var output = markdownService.parse(input, EnumSet.noneOf(Rendering.class), null);

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

	@Test
	void Parse_Heading_NonChatMode_ProducesContentHeader()
	{
		var markdownService = createMarkdownService();

		var output = markdownService.parse("# Title\n", EnumSet.noneOf(Rendering.class), null);

		// Heading produces: ContentHeader("Title") — trailing \n stripped by getContent()
		assertEquals(1, output.size());
		assertInstanceOf(ContentHeader.class, output.getFirst());
		assertEquals("Title", ((Text) output.getFirst().getNode()).getText());
	}

	@Test
	void Parse_Heading_ChatMode_ProducesContentText()
	{
		var markdownService = createMarkdownService();

		var output = markdownService.parse("## Hello\n", EnumSet.of(Rendering.CHAT), null);

		assertEquals(1, output.size());
		assertInstanceOf(ContentText.class, output.getFirst());
		assertEquals("## Hello", ((Text) output.getFirst().getNode()).getText());
	}

	@Test
	void Parse_Heading_Levels()
	{
		var markdownService = createMarkdownService();

		for (int level = 1; level <= 6; level++)
		{
			var output = markdownService.parse("#".repeat(level) + " H\n", EnumSet.noneOf(Rendering.class), null);

			assertEquals(1, output.size());
			assertInstanceOf(ContentHeader.class, output.getFirst());
			assertEquals("H", ((Text) output.getFirst().getNode()).getText());
		}
	}

	@Test
	void Parse_BulletList_ProducesContentTextWithBullet()
	{
		var markdownService = createMarkdownService();

		var output = markdownService.parse("- item\n", EnumSet.noneOf(Rendering.class), null);

		// Should contain a bullet text element
		var hasBullet = output.stream()
				.filter(ContentText.class::isInstance)
				.map(c -> ((Text) c.getNode()).getText())
				.anyMatch(t -> t.contains("• "));
		assertTrue(hasBullet, "Expected bullet character in output");
	}

	@Test
	void Parse_OrderedList_ProducesContentTextWithNumber()
	{
		var markdownService = createMarkdownService();

		var output = markdownService.parse("1. first\n2. second\n", EnumSet.noneOf(Rendering.class), null);

		var hasNumber = output.stream()
				.filter(ContentText.class::isInstance)
				.map(c -> ((Text) c.getNode()).getText())
				.anyMatch(t -> t.contains("1."));
		assertTrue(hasNumber, "Expected numbered list in output");
	}

	@Test
	void Parse_FencedCodeBlock_ProducesContentCode()
	{
		var markdownService = createMarkdownService();

		var input = "```\ncode here\n```\n";
		var output = markdownService.parse(input, EnumSet.noneOf(Rendering.class), null);

		var hasCode = output.stream().anyMatch(ContentCode.class::isInstance);
		assertTrue(hasCode, "Expected ContentCode in output");
	}

	@Test
	void Parse_InlineCode_ProducesContentCode()
	{
		var markdownService = createMarkdownService();

		var output = markdownService.parse("use `println()` here\n", EnumSet.noneOf(Rendering.class), null);

		var hasCode = output.stream().anyMatch(ContentCode.class::isInstance);
		assertTrue(hasCode, "Expected ContentCode for inline code");
	}

	@Test
	void Parse_Strikethrough_ProducesContentStrikethrough()
	{
		var markdownService = createMarkdownService();

		var output = markdownService.parse("~~deleted~~\n", EnumSet.noneOf(Rendering.class), null);

		var hasStrike = output.stream().anyMatch(ContentStrikethrough.class::isInstance);
		assertTrue(hasStrike, "Expected ContentStrikethrough in output");
	}

	@Test
	void Parse_ThematicBreak_NonChatMode_ProducesHorizontalRule()
	{
		var markdownService = createMarkdownService();

		var output = markdownService.parse("---\n", EnumSet.noneOf(Rendering.class), null);

		var hasRule = output.stream().anyMatch(ContentHorizontalRule.class::isInstance);
		assertTrue(hasRule, "Expected ContentHorizontalRule in output");
	}

	@Test
	void Parse_ThematicBreak_ChatMode_ProducesText()
	{
		var markdownService = createMarkdownService();

		var output = markdownService.parse("---\n", EnumSet.of(Rendering.CHAT), null);

		var hasText = output.stream()
				.filter(ContentText.class::isInstance)
				.map(c -> ((Text) c.getNode()).getText())
				.anyMatch(t -> t.contains("---"));
		assertTrue(hasText, "Expected plain text for thematic break in chat mode");
	}

	@Test
	void Parse_EmphasisItalic_ProducesContentEmphasis()
	{
		var markdownService = createMarkdownService();

		var output = markdownService.parse("*italic*\n", EnumSet.noneOf(Rendering.class), null);

		var hasItalic = output.stream()
				.filter(ContentEmphasis.class::isInstance)
				.anyMatch(c -> c.getNode().getStyle().contains("italic"));
		assertTrue(hasItalic, "Expected italic emphasis in output");
	}

	@Test
	void Parse_StrongBold_ProducesContentEmphasis()
	{
		var markdownService = createMarkdownService();

		var output = markdownService.parse("**bold**\n", EnumSet.noneOf(Rendering.class), null);

		var hasBold = output.stream()
				.filter(ContentEmphasis.class::isInstance)
				.anyMatch(c -> c.getNode().getStyle().contains("bold"));
		assertTrue(hasBold, "Expected bold emphasis in output");
	}

	@Test
	void Parse_SoftLineBreak_TextReflow_ConvertsToSpace()
	{
		var markdownService = createMarkdownService();

		var input = "line1\nline2\n";
		var output = markdownService.parse(input, EnumSet.of(Rendering.TEXT_REFLOW), null);

		// With text reflow, the soft break between "line1" and "line2" should be a space, not a newline
		var hasNewline = output.stream()
				.filter(ContentText.class::isInstance)
				.map(c -> ((Text) c.getNode()).getText())
				.anyMatch(t -> t.equals("\n"));
		assertFalse(hasNewline, "Expected no newline with text reflow");
	}

	@Test
	void Parse_SoftLineBreak_NoTextReflow_ProducesNewline()
	{
		var markdownService = createMarkdownService();

		var input = "line1\nline2\n";
		var output = markdownService.parse(input, EnumSet.noneOf(Rendering.class), null);

		var hasNewline = output.stream()
				.filter(ContentText.class::isInstance)
				.map(c -> ((Text) c.getNode()).getText())
				.anyMatch(t -> t.equals("\n"));
		assertTrue(hasNewline, "Expected newline without text reflow");
	}

	@Test
	void Parse_BlockQuote_ProducesQuotedText()
	{
		var markdownService = createMarkdownService();

		var input = "> quoted\n";
		var output = markdownService.parse(input, EnumSet.noneOf(Rendering.class), null);

		var hasQuotePrefix = output.stream()
				.filter(ContentText.class::isInstance)
				.map(c -> ((Text) c.getNode()).getText())
				.anyMatch(t -> t.contains("> "));
		assertTrue(hasQuotePrefix, "Expected quote prefix in output");
	}
}
