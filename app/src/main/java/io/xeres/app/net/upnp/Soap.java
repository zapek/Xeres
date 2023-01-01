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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;

import java.net.URL;
import java.time.Duration;
import java.util.Map;

final class Soap
{
	private static final Logger log = LoggerFactory.getLogger(Soap.class);

	private Soap()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	private static String createSoap(String serviceType, String actionName, Map<String, String> args)
	{
		var soap = new StringBuilder();

		soap.append("<?xml version=\"1.0\"?>\r\n");
		soap.append("<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">");
		soap.append("<s:Body>");
		soap.append("<u:").append(actionName).append(" xmlns:u=\"").append(serviceType).append("\">");

		if (args != null)
		{
			args.forEach((key, value) -> soap.append("<").append(key).append(">").append(value).append("</").append(key).append(">"));
		}

		soap.append("</u:").append(actionName).append(">");
		soap.append("</s:Body>");
		soap.append("</s:Envelope>");

		return soap.toString();
	}

	static ResponseEntity<String> sendRequest(URL controlUrl, String serviceType, String action, Map<String, String> args)
	{
		var webClient = WebClient.builder()
				.baseUrl(controlUrl.toString())
				.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_XML_VALUE)
				.build();

		try
		{
			return webClient.post()
					.bodyValue(createSoap(serviceType, action, args))
					.header("SOAPAction", "\"" + serviceType + "#" + action + "\"")
					.retrieve()
					.toEntity(String.class)
					.block(Duration.ofSeconds(10));
		}
		catch (WebClientException e)
		{
			log.error("Bad request: {}", e.getMessage());
			return ResponseEntity.badRequest().build();
		}
		catch (RuntimeException e)
		{
			log.error("Timeout while sending request: {}", e.getMessage());
			return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).build();
		}
	}
}
