/*
 * Copyright (c) 2019-2023 by David Gerber - https://zapek.com
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

package io.xeres.app.net.upnp;

import io.xeres.testutils.FakeHttpServer;
import io.xeres.testutils.TestUtils;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathNodes;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SoapTest
{
	private static final String SERVICE_TYPE = "urn:schemas-upnp-org:service:WANIPConnection:1";
	private static final String ACTION = "AddPortMapping";

	@Test
	void Soap_NoInstance_OK() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(Soap.class);
	}

	@Test
	void Soap_SendRequest_OK() throws IOException, ParserConfigurationException, SAXException, XPathException
	{
		String KEY1 = "NewExternalPort", KEY2 = "NewProtocol";
		String VALUE1 = "1234", VALUE2 = "TCP";
		var fakeHTTPServer = new FakeHttpServer("/soaptest.xml", HttpURLConnection.HTTP_OK, "OK".getBytes());

		var args = new LinkedHashMap<String, String>(2);
		args.put(KEY1, VALUE1);
		args.put(KEY2, VALUE2);

		var responseEntity = Soap.sendRequest(URI.create("http://localhost:" + fakeHTTPServer.getPort() + "/soaptest.xml"), SERVICE_TYPE, ACTION, args);
		assertEquals("OK", responseEntity.getBody());

		var documentBuilderFactory = DocumentBuilderFactory.newInstance();
		documentBuilderFactory.setNamespaceAware(true);
		var document = documentBuilderFactory.newDocumentBuilder().parse(new ByteArrayInputStream(fakeHTTPServer.getRequestBody()));
		assertEquals("1.0", document.getXmlVersion());

		var xPath = XPathFactory.newInstance().newXPath();
		xPath.setNamespaceContext(createNameSpaceContext(Map.of(
				"s", "http://schemas.xmlsoap.org/soap/envelope/",
				"u", SERVICE_TYPE)));
		var nodes = xPath.evaluateExpression("//s:Envelope//s:Body//u:" + ACTION, document, XPathNodes.class);
		assertEquals(1, nodes.size());

		assertEquals("u:" + ACTION, nodes.get(0).getNodeName());
		var childNodes = nodes.get(0).getChildNodes();
		assertEquals(KEY1, childNodes.item(0).getNodeName());
		assertEquals(VALUE1, childNodes.item(0).getTextContent());
		assertEquals(KEY2, childNodes.item(1).getNodeName());
		assertEquals(VALUE2, childNodes.item(1).getTextContent());

		fakeHTTPServer.shutdown();
	}

	private NamespaceContext createNameSpaceContext(Map<String, String> uris)
	{
		return new NamespaceContext()
		{
			@Override
			public String getNamespaceURI(String prefix)
			{
				return uris.get(prefix);
			}

			@Override
			public String getPrefix(String namespaceURI)
			{
				return null;
			}

			@Override
			public Iterator<String> getPrefixes(String namespaceURI)
			{
				return null;
			}
		};
	}
}
