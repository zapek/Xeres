/*
 * Copyright (c) 2019-2026 by David Gerber - https://zapek.com
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

package io.xeres.chess;

import java.util.*;

public final class ChessResources extends ResourceBundle
{
	private final ResourceBundle translations;

	private ChessResources(ResourceBundle host)
	{
		setParent(host);
		translations = ResourceBundle.getBundle("i18n.chess", host.getLocale(), ChessResources.class.getClassLoader());
	}

	public static ResourceBundle bundle(ResourceBundle host)
	{
		return host instanceof ChessResources ? host : new ChessResources(host);
	}

	public static ResourceBundle forLocale(Locale locale)
	{
		return bundle(ResourceBundle.getBundle("i18n.messages", locale));
	}

	@Override
	public Locale getLocale()
	{
		return parent.getLocale();
	}

	@Override
	protected Object handleGetObject(String key)
	{
		return translations.containsKey(key) ? translations.getObject(key) : null;
	}

	@Override
	public Enumeration<String> getKeys()
	{
		var keys = new HashSet<>(translations.keySet());
		keys.addAll(parent.keySet());
		return Collections.enumeration(keys);
	}
}
