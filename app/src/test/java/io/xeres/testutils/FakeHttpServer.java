/*
 * Copyright (c) 2019-2020 by David Gerber - https://zapek.com
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

package io.xeres.testutils;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;

public class FakeHttpServer
{
	private int port = 1068;
	private final HttpServer httpServer;
	private byte[] requestBody;

	public FakeHttpServer(String path, int responseCode, byte[] responseBody)
	{
		httpServer = createHttpServer();

		HttpHandler handler = exchange -> {
			requestBody = exchange.getRequestBody().readAllBytes();
			exchange.sendResponseHeaders(responseCode, responseBody != null ? responseBody.length : -1);
			if (responseBody != null)
			{
				exchange.getResponseBody().write(responseBody);
			}
			exchange.close();
		};
		httpServer.createContext(path, handler);

		httpServer.start();
	}

	public byte[] getRequestBody()
	{
		return requestBody;
	}

	public void shutdown()
	{
		httpServer.stop(0);
	}

	public int getPort()
	{
		return port;
	}

	private HttpServer createHttpServer()
	{
		var address = new InetSocketAddress(port);
		try
		{
			return HttpServer.create(address, 0);
		}
		catch (BindException e)
		{
			port++;
			return createHttpServer();
		}
		catch (IOException e)
		{
			throw new RuntimeException("I/O error: " + e.getMessage());
		}
	}
}
