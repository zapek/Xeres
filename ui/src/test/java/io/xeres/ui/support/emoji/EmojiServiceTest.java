package io.xeres.ui.support.emoji;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.xeres.ui.properties.UiClientProperties;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class EmojiServiceTest
{
	@Mock
	private UiClientProperties uiClientProperties;

	// We cannot use @InjectMocks because EmojiService performs
	// computations that requires mocks in the constructor and that
	// is executed before "when" statements can be done.
	private EmojiService createEmojiService()
	{
		return new EmojiService(uiClientProperties, new ObjectMapper());
	}


	@ParameterizedTest
	@CsvSource({
			"&#129393;, 1f971",
			"&#127462;&#127464;, 1f1e6-1f1e8",
			"&#128675;&#127997;&#8205;&#9792;&#65039;, 1f6a3-1f3fd-200d-2640-fe0f"
	})
	void EmojiService_CodeDecimalToUnicode_OK(String input, String expected)
	{
		var emojiService = createEmojiService();
		var result = emojiService.codeDecimalToUnicode(input);

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
	void EmojiService_toUnicode_OK(String input, String expected)
	{
		when(uiClientProperties.isColoredEmojis()).thenReturn(false);
		when(uiClientProperties.isRsEmojisAliases()).thenReturn(true);

		var emojiService = createEmojiService();

		var result = emojiService.toUnicode(input);

		assertEquals(expected, result);
	}
}
