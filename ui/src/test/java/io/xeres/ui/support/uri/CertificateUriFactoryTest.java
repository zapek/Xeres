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

import io.xeres.ui.support.contentline.ContentText;
import io.xeres.ui.support.contentline.ContentUri;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;

import static io.xeres.ui.support.uri.UriFactoryUtils.createUriComponentsFromUri;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@ExtendWith(ApplicationExtension.class)
class CertificateUriFactoryTest
{
	@Test
	void CertificateUri_WrongParams_MissingRadix_Fail()
	{
		var url = "retroshare://certificate?name=foo";

		var factory = new CertificateUriFactory();
		var content = factory.create(createUriComponentsFromUri(url), "", null);

		assertInstanceOf(ContentText.class, content);
	}

	@Test
	void CertificateUri_OneParam_Success()
	{
		var url = "retroshare://certificate?radix=abcd0123";

		var factory = new CertificateUriFactory();
		var content = factory.create(createUriComponentsFromUri(url), "", null);

		assertEquals(url, ((ContentUri) content).getUri());
	}

	@Test
	void CertificateUri_TwoParams_Success()
	{
		var url = "retroshare://certificate?radix=abcd0123&name=foo";

		var factory = new CertificateUriFactory();
		var content = factory.create(createUriComponentsFromUri(url), "", null);

		assertEquals(url, ((ContentUri) content).getUri());
	}

	@Test
	void CertificateUri_ThreeParams_Success()
	{
		var url = "retroshare://certificate?radix=abcd0123&name=foo&location=earth";

		var factory = new CertificateUriFactory();
		var content = factory.create(createUriComponentsFromUri(url), "", null);

		assertEquals(url, ((ContentUri) content).getUri());
	}

	@Test
	void CertificateUri_Pretty()
	{
		var url = "retroshare://certificate?radix=abcd0123&name=foo&location=earth";

		var factory = new CertificateUriFactory();
		var content = factory.create(createUriComponentsFromUri(url), "", null);

		assertEquals("Xeres Certificate (foo, @earth)", content.asText());
	}

	@Test
	void CertificateUri_Pretty_FromText()
	{
		var url = "retroshare://certificate?radix=abcd0123&name=foo&location=earth";

		var factory = new CertificateUriFactory();
		var content = factory.create(createUriComponentsFromUri(url), "Test", null);

		assertEquals("Test", content.asText());
	}

	@Test
	void CertificateUri_Pretty_WithoutLocation()
	{
		var url = "retroshare://certificate?radix=abcd0123&name=foo";

		var factory = new CertificateUriFactory();
		var content = factory.create(createUriComponentsFromUri(url), "", null);

		assertEquals("Xeres Certificate (foo)", content.asText());
	}

	@Test
	void CertificateUri_Pretty_NoName()
	{
		var url = "retroshare://certificate?radix=abcd0123";

		var factory = new CertificateUriFactory();
		var content = factory.create(createUriComponentsFromUri(url), "", null);

		assertEquals("Xeres Certificate (unknown)", content.asText());
	}

	@Test
	void CertificateUri_Generate()
	{
		assertEquals("<a href=\"retroshare://certificate?radix=1234&name=foo&location=bar\">Xeres Certificate (foo, @bar)</a>", CertificateUriFactory.generate("1234", "foo", "bar"));
	}
}