package io.xeres.ui.support.markdown;

import io.xeres.ui.FXTest;
import io.xeres.ui.support.contentline.*;
import io.xeres.ui.support.emoji.EmojiService;
import io.xeres.ui.support.markdown.MarkdownService.ParsingMode;
import javafx.scene.control.Hyperlink;
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
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
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
								
				Line 5
				""";

		doAnswer(invocation -> invocation.getArgument(0)).when(emojiService).toUnicode(anyString());

		assertEquals(wanted, markdownService.parse(text, EnumSet.noneOf(ParsingMode.class), null).stream()
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
				> Line3
				""";

		doAnswer(invocation -> invocation.getArgument(0)).when(emojiService).toUnicode(anyString());

		assertEquals(wanted, markdownService.parse(text, EnumSet.noneOf(ParsingMode.class), null).stream()
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
								
				Line3
				""";

		doAnswer(invocation -> invocation.getArgument(0)).when(emojiService).toUnicode(anyString());

		assertEquals(wanted, markdownService.parse(text, EnumSet.noneOf(ParsingMode.class), null).stream()
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

		doAnswer(invocation -> invocation.getArgument(0)).when(emojiService).toUnicode(anyString());

		assertEquals(wanted, markdownService.parse(text, EnumSet.of(ParsingMode.ONE_LINER), null).stream()
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

				Line3 Line4
				""";

		doAnswer(invocation -> invocation.getArgument(0)).when(emojiService).toUnicode(anyString());

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

		doAnswer(invocation -> invocation.getArgument(0)).when(emojiService).toUnicode(anyString());

		var output = markdownService.parse(input, EnumSet.of(ParsingMode.ONE_LINER), null);

		assertEquals(3, output.size());
		assertInstanceOf(ContentText.class, output.get(0));
		assertInstanceOf(ContentUri.class, output.get(1));
		assertInstanceOf(ContentText.class, output.get(2));

		assertEquals("Hello world! ", ((Text) output.get(0).getNode()).getText());
		assertEquals("https://xeres.io", ((Hyperlink) output.get(1).getNode()).getText());
		assertEquals(" is the site to visit now!", ((Text) output.get(2).getNode()).getText());
	}

	@Test
	void ParseInlineUrls_WeirdChars_Success()
	{
		var markdownService = createMarkdownService();

		var input = "https://www.foobar.com/watch?v=aXfS2p_ZyHY";

		doAnswer(invocation -> invocation.getArgument(0)).when(emojiService).toUnicode(anyString());

		var output = markdownService.parse(input, EnumSet.of(ParsingMode.ONE_LINER), null);

		assertEquals(1, output.size());
		assertInstanceOf(ContentUri.class, output.getFirst());

		assertEquals(input, ((Hyperlink) output.getFirst().getNode()).getText());
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

		doAnswer(invocation -> invocation.getArgument(0)).when(emojiService).toUnicode(anyString());

		var output = markdownService.parse(input, EnumSet.of(ParsingMode.ONE_LINER), null);

		assertEquals(expected, ((Text) output.getFirst().getNode()).getText());
	}

	@Test
	void Parse_Empty()
	{
		var markdownService = createMarkdownService();

		var input = "\n";

		doAnswer(invocation -> invocation.getArgument(0)).when(emojiService).toUnicode(anyString());

		var output = markdownService.parse(input, EnumSet.noneOf(ParsingMode.class), null);

		assertEquals(0, output.size());
	}

	@Test
	void Parse_Empty_Too()
	{
		var markdownService = createMarkdownService();

		var input = "\n\n";

		doAnswer(invocation -> invocation.getArgument(0)).when(emojiService).toUnicode(anyString());

		var output = markdownService.parse(input, EnumSet.noneOf(ParsingMode.class), null);

		assertEquals(0, output.size());
	}

	@Test
	void Parse_Simple_Text()
	{
		var markdownService = createMarkdownService();

		var input = "hello, world\n";

		doAnswer(invocation -> invocation.getArgument(0)).when(emojiService).toUnicode(anyString());

		var output = markdownService.parse(input, EnumSet.noneOf(ParsingMode.class), null);

		assertEquals(1, output.size());

		assertInstanceOf(ContentText.class, output.getFirst());
		assertEquals("hello, world\n", ((Text) output.getFirst().getNode()).getText());
	}

	@Test
	void Parse_OneLine_Several()
	{
		var markdownService = createMarkdownService();

		var input = "https://zapek.com :-)\n";

		when(emojiService.toUnicode(input)).thenReturn("https://zapek.com \uD83D\uDE42\n");

		var output = markdownService.parse(input, EnumSet.noneOf(ParsingMode.class), null);

		assertEquals(4, output.size()); // url + " " + 🙂 + "\n"

		assertInstanceOf(ContentUri.class, output.get(0));
		assertEquals("https://zapek.com", ((Hyperlink) output.get(0).getNode()).getText());

		assertInstanceOf(ContentText.class, output.get(1));
		assertEquals(" ", ((Text) output.get(1).getNode()).getText());

		assertInstanceOf(ContentEmoji.class, output.get(2));

		assertInstanceOf(ContentText.class, output.get(3));
		assertEquals("\n", ((Text) output.get(3).getNode()).getText());
	}

	@Test
	void Parse_Multiline_Several()
	{
		var markdownService = createMarkdownService();

		var line1 = "https://zapek.com :-) **yeah**\n";
		var line2 = "and another one: `fork();` it is\n";
		var input = line1 + line2;

		when(emojiService.toUnicode(line1)).thenReturn("https://zapek.com \uD83D\uDE42 **yeah**\n");
		when(emojiService.toUnicode(line2)).thenReturn(line2);

		var output = markdownService.parse(input, EnumSet.noneOf(ParsingMode.class), null);

		assertEquals(9, output.size());

		assertInstanceOf(ContentUri.class, output.get(0));
		assertEquals("https://zapek.com", ((Hyperlink) output.get(0).getNode()).getText());

		assertInstanceOf(ContentText.class, output.get(1));
		assertEquals(" ", ((Text) output.get(1).getNode()).getText());

		assertInstanceOf(ContentEmoji.class, output.get(2));

		assertInstanceOf(ContentText.class, output.get(3));
		assertEquals(" ", ((Text) output.get(3).getNode()).getText());

		assertInstanceOf(ContentEmphasis.class, output.get(4));
		assertEquals("yeah", ((Text) output.get(4).getNode()).getText());
		assertEquals("-fx-font-weight: bold;", output.get(4).getNode().getStyle());

		assertInstanceOf(ContentText.class, output.get(5));
		assertEquals("\n", ((Text) output.get(5).getNode()).getText());

		assertInstanceOf(ContentText.class, output.get(6));
		assertEquals("and another one: ", ((Text) output.get(6).getNode()).getText());

		assertInstanceOf(ContentCode.class, output.get(7));
		assertEquals("fork();", ((Text) output.get(7).getNode()).getText());

		assertInstanceOf(ContentText.class, output.get(8));
		assertEquals(" it is\n", ((Text) output.get(8).getNode()).getText());
	}
}
