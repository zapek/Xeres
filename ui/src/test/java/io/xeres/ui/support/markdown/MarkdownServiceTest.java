package io.xeres.ui.support.markdown;

import io.xeres.ui.support.contentline.Content;
import io.xeres.ui.support.contentline.ContentText;
import io.xeres.ui.support.contentline.ContentUri;
import io.xeres.ui.support.emoji.EmojiService;
import io.xeres.ui.support.markdown.MarkdownService.ParsingMode;
import javafx.scene.control.Hyperlink;
import javafx.scene.text.Text;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.EnumSet;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

@ExtendWith(SpringExtension.class)
class MarkdownServiceTest
{
	@Mock
	private EmojiService emojiService;

	@InjectMocks
	private MarkdownService markdownService;

	@Test
	void MarkdownService_Parse_Sanitize_Default_OK()
	{
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
				Line4
				""";

		doAnswer(invocation -> invocation.getArgument(0)).when(emojiService).toUnicode(anyString());

		assertEquals(wanted, markdownService.parse(text, EnumSet.noneOf(ParsingMode.class)).stream()
				.map(Content::asText)
				.collect(Collectors.joining()));
	}

	@Test
	void MarkdownService_Parse_Sanitize_Default2_OK()
	{
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

		assertEquals(wanted, markdownService.parse(text, EnumSet.noneOf(ParsingMode.class)).stream()
				.map(Content::asText)
				.collect(Collectors.joining()));
	}

	@Test
	void MarkdownService_Sanitize_NoEndOfLine_OK()
	{
		var text = """
				Line1



				Line2 with trails  \s

				Line3
				Line4
				""";

		var wanted = """
				Line1Line2 with trailsLine3Line4""";

		doAnswer(invocation -> invocation.getArgument(0)).when(emojiService).toUnicode(anyString());

		assertEquals(wanted, markdownService.parse(text, EnumSet.of(ParsingMode.ONE_LINER)).stream()
				.map(Content::asText)
				.collect(Collectors.joining()));
	}

	@Test
	void MarkdownService_Sanitize_Paragraph_OK()
	{
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

		assertEquals(wanted, markdownService.parse(text, EnumSet.of(ParsingMode.PARAGRAPH)).stream()
				.map(Content::asText)
				.collect(Collectors.joining()));
	}

	@Test
	void MarkdownService_ParseInlineUrls_OK()
	{
		var input = "Hello world! https://xeres.io is the site to visit now!";

		doAnswer(invocation -> invocation.getArgument(0)).when(emojiService).toUnicode(anyString());

		var output = markdownService.parse(input, EnumSet.of(ParsingMode.ONE_LINER));

		assertEquals(3, output.size());
		assertInstanceOf(ContentText.class, output.get(0));
		assertInstanceOf(ContentUri.class, output.get(1));
		assertInstanceOf(ContentText.class, output.get(2));

		assertEquals("Hello world! ", ((Text) output.get(0).getNode()).getText());
		assertEquals("https://xeres.io", ((Hyperlink) output.get(1).getNode()).getText());
		assertEquals(" is the site to visit now!", ((Text) output.get(2).getNode()).getText());
	}

	@ParameterizedTest
	@CsvSource({
			"    foo();, foo();",
			"\tfoo();, foo();",
			"        foo();,     foo();"
	})
	void MarkdownService_RemoveFirstStartingSpacesCode(String input, String expected)
	{
		doAnswer(invocation -> invocation.getArgument(0)).when(emojiService).toUnicode(anyString());

		var output = markdownService.parse(input, EnumSet.of(ParsingMode.ONE_LINER));

		assertEquals(expected, ((Text) output.get(0).getNode()).getText());
	}
}
