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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

final class Device implements DeviceSpecs
{
	private static final Logger log = LoggerFactory.getLogger(Device.class);

	private static final Pattern HTTP_OK_PATTERN = Pattern.compile("^HTTP/1\\.. 200 OK");
	private static final int MAX_HEADER_VALUE_LENGTH = 128;

	private static final Set<HttpuHeader> supportedHeaders = EnumSet.allOf(HttpuHeader.class);

	private InetSocketAddress inetSocketAddress;
	private Map<HttpuHeader, String> headers;

	private String modelName;
	private String manufacturer;
	private URI manufacturerUrl;
	private String serialNumber;
	private URI presentationUrl;
	private URI controlUrl;
	private URI locationUrl;
	private String serviceType;
	private boolean hasControlPoint;
	private final HashSet<PortMapping> ports = new HashSet<>();

	static Device from(SocketAddress socketAddress, ByteBuffer byteBuffer)
	{
		if (!(socketAddress instanceof InetSocketAddress))
		{
			log.warn("Not an Inet device. Ignoring.");
			return Device.fromInvalid();
		}

		var reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(byteBuffer.array()), StandardCharsets.US_ASCII));

		try
		{
			Map<HttpuHeader, String> headers = new EnumMap<>(HttpuHeader.class);
			var s = reader.readLine();

			if (!HTTP_OK_PATTERN.matcher(s).matches())
			{
				log.warn("Not a valid HTTP response: {}. Ignoring.", s);
				return Device.fromInvalid();
			}

			while ((s = reader.readLine()) != null)
			{
				var tokens = s.split(":", 2);
				if (tokens.length != 2 || tokens[1].length() > MAX_HEADER_VALUE_LENGTH)
				{
					continue;
				}

				var header = tokens[0].toUpperCase(Locale.ROOT).strip();
				if (supportedHeaders.stream().anyMatch(h -> h.name().equals(header)))
				{
					headers.put(HttpuHeader.valueOf(header), tokens[1].strip());
				}
			}
			return new Device(socketAddress, headers);
		}
		catch (IOException e)
		{
			log.warn("Couldn't read line, shouldn't happen", e);
		}
		return Device.fromInvalid();
	}

	private static Device fromInvalid()
	{
		return new Device();
	}

	private Device(SocketAddress socketAddress, Map<HttpuHeader, String> headers)
	{
		inetSocketAddress = (InetSocketAddress) socketAddress;
		this.headers = headers;
	}

	private Device()
	{

	}

	public boolean isValid()
	{
		return inetSocketAddress != null && hasLocation();
	}

	public boolean isInvalid()
	{
		return inetSocketAddress == null;
	}

	public InetSocketAddress getInetSocketAddress()
	{
		return inetSocketAddress;
	}

	public Optional<String> getHeaderValue(HttpuHeader header)
	{
		if (isInvalid())
		{
			return Optional.empty();
		}
		return Optional.ofNullable(headers.get(header));
	}

	public boolean hasLocation()
	{
		return getLocationUrl() != null;
	}

	public URI getLocationUrl()
	{
		if (locationUrl != null)
		{
			return locationUrl;
		}

		locationUrl = getHeaderValue(HttpuHeader.LOCATION).map(s -> {
			try
			{
				return new URI(s);
			}
			catch (URISyntaxException e)
			{
				log.error("UPNP: unparseable URL {}, {}", s, e.getMessage());
				return null;
			}
		}).orElse(null);
		return locationUrl;
	}

	public boolean hasServer()
	{
		return getHeaderValue(HttpuHeader.SERVER).isPresent();
	}

	public String getServer()
	{
		return getHeaderValue(HttpuHeader.SERVER).orElse(null);
	}

	public boolean hasUsn()
	{
		return getHeaderValue(HttpuHeader.USN).isPresent();
	}

	public String getUsn()
	{
		return getHeaderValue(HttpuHeader.USN).orElse(null);
	}

	@Override
	public boolean hasModelName()
	{
		return modelName != null;
	}

	@Override
	public String getModelName()
	{
		return modelName;
	}

	@Override
	public void setModelName(String modelName)
	{
		this.modelName = modelName;
	}

	@Override
	public boolean hasManufacturer()
	{
		return manufacturer != null;
	}

	@Override
	public String getManufacturer()
	{
		return manufacturer;
	}

	@Override
	public void setManufacturer(String manufacturer)
	{
		this.manufacturer = manufacturer;
	}

	@Override
	public URI getManufacturerUrl()
	{
		return manufacturerUrl;
	}

	@Override
	public void setManufacturerUrl(String manufacturerUrl)
	{
		this.manufacturerUrl = parseUrl(manufacturerUrl);
	}

	@Override
	public boolean hasSerialNumber()
	{
		return serialNumber != null;
	}

	@Override
	public String getSerialNumber()
	{
		return serialNumber;
	}

	@Override
	public void setSerialNumber(String serialNumber)
	{
		this.serialNumber = serialNumber;
	}

	@Override
	public boolean hasControlUrl()
	{
		return controlUrl != null;
	}

	@Override
	public URI getControlUrl()
	{
		return controlUrl;
	}

	@Override
	public void setControlUrl(String controlUrl)
	{
		this.controlUrl = parseUrl(locationUrl, controlUrl);
	}

	@Override
	public boolean hasPresentationUrl()
	{
		return presentationUrl != null;
	}

	@Override
	public URI getPresentationUrl()
	{
		return presentationUrl;
	}

	@Override
	public void setPresentationUrl(String presentationUrl)
	{
		this.presentationUrl = parseUrl(presentationUrl);
	}

	@Override
	public String getServiceType()
	{
		return serviceType;
	}

	@Override
	public void setServiceType(String serviceType)
	{
		this.serviceType = serviceType;
	}

	public void addControlPoint()
	{
		if (isInvalid())
		{
			throw new IllegalStateException("Trying to add a control point to an invalid device");
		}
		hasControlPoint = ControlPoint.updateDevice(this, getLocationUrl());
	}

	public boolean hasControlPoint()
	{
		return hasControlPoint;
	}

	public boolean addPortMapping(String internalIp, int internalPort, int externalPort, int duration, Protocol protocol)
	{
		var added = ControlPoint.addPortMapping(getControlUrl(), getServiceType(), internalIp, internalPort, externalPort, duration, protocol);
		if (added)
		{
			ports.add(new PortMapping(externalPort, protocol));
		}
		return added;
	}

	public void deletePortMapping(int externalPort, Protocol protocol)
	{
		if (ControlPoint.removePortMapping(getControlUrl(), getServiceType(), externalPort, protocol))
		{
			ports.removeIf(portMapping -> portMapping.port() == externalPort && portMapping.protocol() == protocol);
		}
	}

	public void removeAllPortMapping()
	{
		new HashSet<>(ports).forEach(portMapping -> deletePortMapping(portMapping.port(), portMapping.protocol()));
	}

	public String getExternalIpAddress()
	{
		return ControlPoint.getExternalIpAddress(getControlUrl(), getServiceType());
	}

	private static URI parseUrl(String url)
	{
		return parseUrl(null, url);
	}

	private static URI parseUrl(URI baseUrl, String url)
	{
		try
		{
			if (baseUrl != null)
			{
				return baseUrl.resolve(url);
			}
			return new URI(addProtocolIfMissing(url));
		}
		catch (URISyntaxException e)
		{
			log.error("Wrong URL {}, {}", url, e.getMessage());
			return null;
		}
	}

	/**
	 * Fixes the URL returned by some routers that miss a protocol, for
	 * example www.Nucom.com
	 * @param url the url
	 * @return an url with the protocol prepended
	 */
	private static String addProtocolIfMissing(String url)
	{
		if (url != null && url.toLowerCase(Locale.ROOT).startsWith("www."))
		{
			return "https://" + url;
		}
		return url;
	}

	@Override
	public String toString()
	{
		return "Device{" +
				"inetSocketAddress=" + inetSocketAddress +
				", headers=" + headers +
				", modelName='" + modelName + '\'' +
				", manufacturer='" + manufacturer + '\'' +
				", manufacturerUrl=" + manufacturerUrl +
				", serialNumber='" + serialNumber + '\'' +
				", presentationUrl=" + presentationUrl +
				", controlUrl=" + controlUrl +
				", locationUrl=" + locationUrl +
				", serviceType='" + serviceType + '\'' +
				", hasControlPoint=" + hasControlPoint +
				", ports=" + ports +
				'}';
	}
}
