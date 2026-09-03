/*
 * Copyright (c) 2026 by David Gerber - https://zapek.com
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

package io.xeres.app.util;

import io.xeres.testutils.TestUtils;
import org.junit.jupiter.api.Test;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class XmlUtilsTest
{
	@Test
	void Instance_Throws() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(XmlUtils.class);
	}

	@Test
	void GetSecureDocumentBuilderFactory_ReturnsNonNull()
	{
		var factory = XmlUtils.getSecureDocumentBuilderFactory();
		assertNotNull(factory);
	}

	@Test
	void GetSecureDocumentBuilderFactory_DisablesExternalDtd()
	{
		var factory = XmlUtils.getSecureDocumentBuilderFactory();
		assertEquals("", factory.getAttribute(XMLConstants.ACCESS_EXTERNAL_DTD));
	}

	@Test
	void GetSecureDocumentBuilderFactory_DisablesExternalSchema()
	{
		var factory = XmlUtils.getSecureDocumentBuilderFactory();
		assertEquals("", factory.getAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA));
	}

	@Test
	void GetSecureXMLInputFactory_ReturnsNonNull()
	{
		var factory = XmlUtils.getSecureXMLInputFactory();
		assertNotNull(factory);
	}

	@Test
	void GetSecureXMLInputFactory_DisablesDtd()
	{
		var factory = XmlUtils.getSecureXMLInputFactory();
		assertEquals(Boolean.FALSE, factory.getProperty(XMLInputFactory.SUPPORT_DTD));
	}

	@Test
	void GetSecureXMLInputFactory_DisablesExternalEntities()
	{
		var factory = XmlUtils.getSecureXMLInputFactory();
		assertEquals(Boolean.FALSE, factory.getProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES));
	}
}
