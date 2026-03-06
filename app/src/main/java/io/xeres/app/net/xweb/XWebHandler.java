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

package io.xeres.app.net.xweb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

public class XWebHandler extends URLStreamHandler
{
	private static final Logger log = LoggerFactory.getLogger(XWebHandler.class);

	@Override
	protected URLConnection openConnection(URL u) throws IOException
	{
		return new URLConnection(u)
		{
			private static final String DATA = """
					<h1>Hello there!</h1>
					<p>This is the <b>custom</b> protocol that could be used for hidden sites.
					<ul>
					<li>Simple
					<li>Fast
					<li>Easy
					</ul>
					""";
			private boolean connected;

			@Override
			public void connect() throws IOException
			{
				connected = true;
				log.debug("Opening connection to {}", u);
			}

			@Override
			public InputStream getInputStream() throws IOException
			{
				if (!connected)
				{
					connect();
				}
				return new ByteArrayInputStream(DATA.getBytes());
			}

			@Override
			public OutputStream getOutputStream() throws IOException
			{
				if (!connected)
				{
					connect();
				}
				return new ByteArrayOutputStream();
			}

			@Override
			public int getContentLength()
			{
				return DATA.length();
			}
		};
	}
}
