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

package io.xeres.app.net.protocol;

import java.io.Serial;
import java.net.SocketAddress;

public final class DomainNameSocketAddress extends SocketAddress
{
	@Serial
	private static final long serialVersionUID = -551345992744929084L;
	
	private final String name;

	private DomainNameSocketAddress(String name)
	{
		if (name.contains(":"))
		{
			throw new IllegalArgumentException("DomainNameSocketAddress is only usable for domains alone, not domain/ports");
		}
		else
		{
			this.name = name;
		}
	}

	public static DomainNameSocketAddress of(String name)
	{
		return new DomainNameSocketAddress(name);
	}

	public String getName()
	{
		return name;
	}
}
