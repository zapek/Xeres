package io.xeres.ui.support.markdown;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Markdown2FlowTest
{
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
}
