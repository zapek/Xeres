package io.xeres.ui.support.markdown;

import io.xeres.ui.support.contentline.ContentText;
import io.xeres.ui.support.contentline.ContentUri;
import io.xeres.ui.support.emoji.EmojiService;
import javafx.scene.control.Hyperlink;
import javafx.scene.text.Text;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

class Markdown2FlowTest
{
	private final EmojiService emojiService = mock(EmojiService.class);

	@Test
	void Markdown2Flow_Sanitize_OK()
	{
		var text = """
				Line1



				Line2 with trails  \s

				Line3
				""";

		var wanted = """
				Line1

				Line2 with trails

				Line3""";

		assertEquals(wanted, Markdown2Flow.sanitize(text));
	}

	@ParameterizedTest
	@CsvSource({
			"hello world,hello world",
			";-),\uD83D\uDE09",
			":wink:,\uD83D\uDE09"
	})
	void Markdown2Flow_Parse_Text(String input, String expected)
	{
		var md = new Markdown2Flow(input, emojiService);
		md.setOneLineMode(true);

		var result = md.getContent();

		assertEquals(1, result.size());
		assertInstanceOf(ContentText.class, result.get(0));
		assertEquals(expected, ((Text) result.get(0).getNode()).getText());
	}

	@Test
	void Markdown2Flow_ParseInlineUrls_OK()
	{
		var input = "Hello world! https://xeres.io is the site to visit now!";

		var md = new Markdown2Flow(input, emojiService);
		md.setOneLineMode(true);

		var output = md.getContent();

		assertEquals(3, output.size());
		assertInstanceOf(ContentText.class, output.get(0));
		assertInstanceOf(ContentUri.class, output.get(1));
		assertInstanceOf(ContentText.class, output.get(2));

		assertEquals("Hello world! ", ((Text) output.get(0).getNode()).getText());
		assertEquals("https://xeres.io", ((Hyperlink) output.get(1).getNode()).getText());
		assertEquals(" is the site to visit now!", ((Text) output.get(2).getNode()).getText());
	}
}
