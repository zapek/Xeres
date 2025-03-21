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

package io.xeres.common.properties;

import io.xeres.common.protocol.ip.IP;
import org.apache.commons.lang3.StringUtils;

public final class StartupProperties
{
	public enum Property
	{
		SERVER_ONLY("xrs.network.server-only", Boolean.class, Origin.PROPERTY),
		CONTROL_PORT("server.port", Integer.class, Origin.PROPERTY),
		CONTROL_ADDRESS("server.address", String.class, Origin.PROPERTY),
		CONTROL_PASSWORD("xrs.server.password", Boolean.class, Origin.PROPERTY),
		SERVER_ADDRESS("xrs.network.server-address", String.class, Origin.PROPERTY),
		SERVER_PORT("xrs.network.server-port", Integer.class, Origin.PROPERTY),
		DATA_DIR("xrs.data.dir-path", String.class, Origin.PROPERTY),
		UI("xrs.ui.enabled", Boolean.class, Origin.PROPERTY),
		UI_ADDRESS("xrs.ui.address", String.class, Origin.PROPERTY),
		UI_PORT("xrs.ui.port", Integer.class, Origin.PROPERTY),
		ICONIFIED("xrs.ui.iconified", Boolean.class, Origin.PROPERTY),
		FAST_SHUTDOWN("xrs.network.fast-shutdown", Boolean.class, Origin.PROPERTY),
		REMOTE_PASSWORD("xrs.ui.remote-password", String.class, Origin.PROPERTY),
		HTTPS("server.ssl.enabled", Boolean.class, Origin.PROPERTY);

		Property(String propertyName, Class<?> javaClass, Origin origin)
		{
			this.propertyName = propertyName;
			this.javaClass = javaClass;
			this.origin = origin;
		}

		private final String propertyName;
		private final Class<?> javaClass;
		private Origin origin;

		public String getKey()
		{
			return propertyName;
		}

		public Class<?> getJavaClass()
		{
			return javaClass;
		}

		public Origin getOrigin()
		{
			return origin;
		}

		private void setOrigin(Origin origin)
		{
			this.origin = origin;
		}

		/**
		 * Checks if an argument was set by command line or environment variable.
		 *
		 * @return true if set by env var or command line
		 */
		public boolean isUnset()
		{
			return origin == Origin.PROPERTY;
		}
	}

	public enum Origin
	{
		PROPERTY,
		ENVIRONMENT_VARIABLE,
		ARGUMENT
	}

	private StartupProperties()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static String getString(Property property, String defaultValue)
	{
		return System.getProperty(property.getKey(), defaultValue);
	}

	public static String getString(Property property)
	{
		return System.getProperty(property.getKey());
	}

	public static void setString(Property property, String value, Origin origin)
	{
		if (!property.getJavaClass().equals(String.class))
		{
			throw new IllegalArgumentException("Property class for " + property.getKey() + " must be a String but it's a " + property.getJavaClass());
		}

		if (StringUtils.isBlank(value))
		{
			throw new IllegalArgumentException("Property " + property.name() + " (" + property.getKey() + ") does not contain a value");
		}

		property.setOrigin(origin);
		System.setProperty(property.getKey(), value);
	}

	@SuppressWarnings("java:S2447")
	public static Boolean getBoolean(Property property)
	{
		var value = System.getProperty(property.getKey());
		if (value == null)
		{
			return null;
		}
		return Boolean.parseBoolean(value);
	}

	public static boolean getBoolean(Property property, boolean defaultValue)
	{
		var value = System.getProperty(property.getKey());
		if (value == null)
		{
			return defaultValue;
		}
		return Boolean.parseBoolean(value);
	}

	public static void setBoolean(Property property, String value, Origin origin)
	{
		if (!property.getJavaClass().equals(Boolean.class))
		{
			throw new IllegalArgumentException("Property class for " + property.getKey() + " must be a Boolean but it's a " + property.getJavaClass());
		}

		var val = value.equals("1") || value.equalsIgnoreCase("yes") || Boolean.parseBoolean(value);
		if (!val && !(value.equals("0") || value.equalsIgnoreCase("no") || value.equalsIgnoreCase("false")))
		{
			throw new IllegalArgumentException("Property " + property.name() + " (" + property.getKey() + ") does not contain a boolean value (" + value + ")");
		}
		property.setOrigin(origin);
		System.setProperty(property.getKey(), String.valueOf(val));
	}

	public static Integer getInteger(Property property)
	{
		var value = System.getProperty(property.getKey());
		if (value == null)
		{
			return null;
		}
		return Integer.parseInt(value);
	}

	public static void setPort(Property property, String value, Origin origin)
	{
		if (!property.getJavaClass().equals(Integer.class))
		{
			throw new IllegalArgumentException("Property class for " + property.getKey() + " must be an Integer but it's a " + property.getJavaClass());
		}

		try
		{
			var val = Integer.parseUnsignedInt(value);
			if (IP.isInvalidPort(val))
			{
				throw new NumberFormatException();
			}
			property.setOrigin(origin);
			System.setProperty(property.getKey(), String.valueOf(val));
		}
		catch (NumberFormatException e)
		{
			throw new IllegalArgumentException("Property " + property.name() + " (" + property.getKey() + ") does not contain a port bigger than 0 and smaller than 65536 (" + value + ")");
		}
	}

	public static void setUiRemoteConnect(String ipAndPort, Origin origin)
	{
		var tokens = ipAndPort.split(":");

		if (StringUtils.isBlank(tokens[0]))
		{
			throw new IllegalArgumentException("Missing hostname");
		}
		if (!IP.isBindableIp(tokens[0]))
		{
			throw new IllegalArgumentException("IP " + tokens[0] + " cannot be bound to");
		}
		setString(Property.UI_ADDRESS, tokens[0], origin);

		if (tokens.length == 2 && StringUtils.isNotBlank(tokens[1]))
		{
			if (IP.isInvalidPort(Integer.parseUnsignedInt(tokens[1])))
			{
				throw new IllegalArgumentException("Invalid port " + tokens[1]);
			}
			setPort(Property.UI_PORT, tokens[1], origin);
		}
		System.setProperty("spring.main.web-application-type", "none");
	}
}
