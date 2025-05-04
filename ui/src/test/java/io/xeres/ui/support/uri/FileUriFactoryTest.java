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

package io.xeres.ui.support.uri;

import io.xeres.testutils.Sha1SumFakes;
import io.xeres.ui.support.contentline.ContentText;
import io.xeres.ui.support.contentline.ContentUri;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testfx.framework.junit5.ApplicationExtension;

import static io.xeres.ui.support.uri.UriFactoryUtils.createUriComponentsFromUri;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@ExtendWith(ApplicationExtension.class)
class FileUriFactoryTest
{
	@ParameterizedTest
	@ValueSource(strings = {
			"retroshare://file?size=128&hash=123400000000000000000000000000000000789a", // missing name
			"retroshare://file?name=foo&hash=123400000000000000000000000000000000789a", // missing size
			"retroshare://file?name=foo&size=128" // missing hash
	})
	void FileUri_WrongParams_Fail(String url)
	{
		var factory = new FileUriFactory();
		var content = factory.create(createUriComponentsFromUri(url), "", null);

		assertInstanceOf(ContentText.class, content);
	}

	@Test
	void FileUri_Success()
	{
		var url = "retroshare://file?name=foo&size=128&hash=123400000000000000000000000000000000789a";

		var factory = new FileUriFactory();
		var content = factory.create(createUriComponentsFromUri(url), "", uri -> {
		});

		assertEquals(url, ((ContentUri) content).getUri());
	}

	@Test
	void FileUri_Pretty()
	{
		var url = "retroshare://file?name=foo&size=128&hash=123400000000000000000000000000000000789a";

		var factory = new FileUriFactory();
		var content = factory.create(createUriComponentsFromUri(url), "", uri -> {
		});

		assertEquals("foo (128 bytes)", content.asText());
	}

	@Test
	void FileUri_Pretty_FromText()
	{
		var url = "retroshare://file?name=foo&size=128&hash=123400000000000000000000000000000000789a";

		var factory = new FileUriFactory();
		var content = factory.create(createUriComponentsFromUri(url), "Test", uri -> {
		});

		assertEquals("Test", content.asText());
	}

	@Test
	void FileUri_Generate_Success()
	{
		var hash = Sha1SumFakes.createSha1Sum();

		var result = FileUriFactory.generate("foo", 128, hash);

		assertEquals("<a href=\"retroshare://file?name=foo&size=128&hash=" + hash + "\">foo (128 bytes)</a>", result);
	}
}
