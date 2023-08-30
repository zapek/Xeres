package io.xeres.ui.support.emoji;

import io.xeres.ui.properties.UiClientProperties;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
class EmojiServiceTest
{
	@Mock
	private UiClientProperties uiClientProperties;

	@InjectMocks
	private EmojiService emojiService;

	@ParameterizedTest
	@CsvSource({
			"&#129393;, 1f971",
			"&#127462;&#127464;, 1f1e6-1f1e8",
			"&#128675;&#127997;&#8205;&#9792;&#65039;, 1f6a3-1f3fd-200d-2640-fe0f"
	})
	void EmojiService_CodeDecimalToUnicode_OK(String input, String expected)
	{
		var result = emojiService.codeDecimalToUnicode(input);

		assertEquals(expected, result);
	}
}
