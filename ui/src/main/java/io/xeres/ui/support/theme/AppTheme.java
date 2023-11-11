/*
 * Copyright (c) 2023 by David Gerber - https://zapek.com
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

package io.xeres.ui.support.theme;

import atlantafx.base.theme.*;

import java.util.Arrays;

public enum AppTheme
{
	PRIMER_LIGHT("Primer Light", PrimerLight.class),
	PRIMER_DARK("Primer Dark", PrimerDark.class),
	NORD_LIGHT("Nord Light", NordLight.class),
	NORD_DARK("Nord Dark", NordDark.class),
	CUPERTINO_LIGHT("Cupertino Light", CupertinoLight.class),
	CUPERTINO_DARK("Cupertino Dark", CupertinoDark.class),
	DRACULA("Dracula", Dracula.class);

	private final String name;
	private final Class<? extends Theme> themeClass;

	AppTheme(String name, Class<? extends Theme> themeClass)
	{
		this.name = name;
		this.themeClass = themeClass;
	}

	public String getName()
	{
		return name;
	}

	public Class<? extends Theme> getThemeClass()
	{
		return themeClass;
	}

	public static AppTheme findByName(String name)
	{
		return Arrays.stream(values()).filter(appTheme -> appTheme.getName().equals(name)).findFirst().orElse(PRIMER_LIGHT);
	}

	@Override
	public String toString()
	{
		return name;
	}
}
