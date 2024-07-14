package io.xeres.ui.support.uri;

import io.xeres.testutils.Sha1SumFakes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FileContentParserTest
{
	@Test
	void FileContentParser_generate_OK()
	{
		var hash = Sha1SumFakes.createSha1Sum();

		var result = FileContentParser.generate("foo", 128, hash);

		assertEquals("<a href=\"retroshare://file?name=foo&size=128&hash=" + hash + "\">foo</a>", result);
	}
}