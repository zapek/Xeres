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

import io.xeres.common.AppName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathNodes;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

final class ControlPoint
{
	private static final Logger log = LoggerFactory.getLogger(ControlPoint.class);

	private ControlPoint()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	static boolean updateDevice(DeviceSpecs upnpDevice, URI location)
	{
		var controlPointFound = false;

		try
		{
			var document = getDocumentBuilderFactory().newDocumentBuilder().parse(location.toString());
			var xPath = XPathFactory.newInstance().newXPath();

			var devices = xPath.evaluateExpression("//device[deviceType[contains(text(), 'InternetGatewayDevice')]]", document, XPathNodes.class);

			getDeviceInfo(upnpDevice, devices);

			var services = xPath.evaluateExpression("//service[serviceType[contains(text(), 'WANIPConnection') or contains(text(), 'WANPPPConnection')]]", document, XPathNodes.class);

			controlPointFound = hasServices(upnpDevice, services);
		}
		catch (FileNotFoundException e)
		{
			log.error("UPNP router's URL {} is not accessible", location);
		}
		catch (ParserConfigurationException e)
		{
			log.error("Couldn't create XML parser for UPNP router URL {}: {}", location, e.getMessage());
		}
		catch (SAXException e)
		{
			log.error("XML parse error for UPNP router URL {}: {}", location, e.getMessage());
		}
		catch (XPathException e)
		{
			throw new IllegalArgumentException("XPath expression error: " + e.getMessage(), e);
		}
		catch (IOException e)
		{
			log.error("I/O error when parsing UPNP's router URL {}: {}", location, e.getMessage());
		}
		return controlPointFound;
	}

	private static void getDeviceInfo(DeviceSpecs upnpDevice, XPathNodes devices) throws XPathException
	{
		if (devices.size() != 1)
		{
			throw new IllegalStateException("Require 1 root device, found: " + devices.size());
		}

		var childNodes = devices.get(0).getChildNodes();
		for (var i = 0; i < childNodes.getLength(); i++)
		{
			var item = childNodes.item(i);
			switch (item.getNodeName().toLowerCase(Locale.ROOT))
			{
				case "modelname" -> upnpDevice.setModelName(item.getTextContent().trim());
				case "manufacturer" -> upnpDevice.setManufacturer(item.getTextContent().trim());
				case "manufacturerurl" -> upnpDevice.setManufacturerUrl(item.getTextContent().trim());
				case "serialnumber" -> upnpDevice.setSerialNumber(item.getTextContent().trim());
				case "presentationurl" -> upnpDevice.setPresentationUrl(item.getTextContent().trim());
				default -> log.trace("node: {}", item.getNodeName());
			}
		}
	}

	private static boolean hasServices(DeviceSpecs upnpDevice, XPathNodes services) throws XPathException
	{
		var controlUrlFound = false;

		if (services.size() != 1)
		{
			throw new IllegalStateException("More than one service: " + services.size());
		}

		var childNodes = services.get(0).getChildNodes();
		for (var i = 0; i < childNodes.getLength(); i++)
		{
			var item = childNodes.item(i);
			switch (item.getNodeName().toLowerCase(Locale.ROOT))
			{
				case "controlurl" -> {
					upnpDevice.setControlUrl(item.getTextContent().trim());
					controlUrlFound = true;
				}
				case "servicetype" -> upnpDevice.setServiceType(item.getTextContent().trim());
				default -> log.trace("service: {}", item.getNodeName());
			}
		}
		return controlUrlFound;
	}

	static boolean addPortMapping(URI controlUrl, String serviceType, String internalIp, int internalPort, int externalPort, int duration, Protocol protocol)
	{
		Map<String, String> args = HashMap.newHashMap(8);
		args.put("NewRemoteHost", "");
		args.put("NewExternalPort", String.valueOf(externalPort));
		args.put("NewProtocol", protocol.name());
		args.put("NewInternalPort", String.valueOf(internalPort));
		args.put("NewInternalClient", internalIp);
		args.put("NewEnabled", "1");
		args.put("NewPortMappingDescription", AppName.NAME + " " + protocol.name());
		args.put("NewLeaseDuration", String.valueOf(duration));

		var response = Soap.sendRequest(controlUrl, serviceType, "AddPortMapping", args);
		return response.getStatusCode() == HttpStatus.OK;
	}

	static boolean removePortMapping(URI controlUrl, String serviceType, int externalPort, Protocol protocol)
	{
		Map<String, String> args = HashMap.newHashMap(3);
		args.put("NewRemoteHost", "");
		args.put("NewExternalPort", String.valueOf(externalPort));
		args.put("NewProtocol", protocol.name());

		var response = Soap.sendRequest(controlUrl, serviceType, "DeletePortMapping", args);
		return response.getStatusCode() == HttpStatus.OK;
	}

	static String getExternalIpAddress(URI controlUrl, String serviceType)
	{
		var response = Soap.sendRequest(controlUrl, serviceType, "GetExternalIPAddress", null);
		var body = response.getBody();
		if (response.getStatusCode() == HttpStatus.OK && body != null)
		{
			var reply = getTextNodes(body);
			return reply.getOrDefault("NewExternalIPAddress", "");
		}
		return "";
	}

	static Map<String, String> getTextNodes(String xml)
	{
		Map<String, String> result = new HashMap<>();
		try
		{
			var document = getDocumentBuilderFactory().newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes()));
			var xPath = XPathFactory.newInstance().newXPath();
			var textNodes = xPath.evaluateExpression("//text()", document, XPathNodes.class);

			for (var textNode : textNodes)
			{
				result.put(textNode.getParentNode().getNodeName(), textNode.getTextContent());
			}
		}
		catch (SAXException e)
		{
			throw new IllegalArgumentException("XML parse error on UPNP router reply: " + e.getMessage(), e);
		}
		catch (IOException e)
		{
			throw new IllegalArgumentException("I/O error when parsing UPNP router's XML reply: " + e.getMessage(), e);
		}
		catch (ParserConfigurationException e)
		{
			throw new IllegalArgumentException("Couldn't create XML parser for UPNP router's XML reply: " + e.getMessage(), e);
		}
		catch (XPathExpressionException e)
		{
			throw new IllegalArgumentException("XPath expression error: " + e.getMessage(), e);
		}
		return result;
	}

	private static DocumentBuilderFactory getDocumentBuilderFactory()
	{
		var df = DocumentBuilderFactory.newInstance();
		df.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
		df.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
		return df;
	}
}
