package io.xeres.ui.support.uri;

import io.xeres.testutils.Sha1SumFakes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FileUriFactoryTest
{
	@Test
	void Generate_Success()
	{
		var hash = Sha1SumFakes.createSha1Sum();

		var result = FileUriFactory.generate("foo", 128, hash);

		assertEquals("<a href=\"retroshare://file?name=foo&size=128&hash=" + hash + "\">foo</a>", result);
	}
}