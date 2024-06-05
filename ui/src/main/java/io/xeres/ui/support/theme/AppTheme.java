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
	PRIMER_LIGHT("Primer Light", PrimerLight.class, false),
	PRIMER_DARK("Primer Dark", PrimerDark.class, true),
	NORD_LIGHT("Nord Light", NordLight.class, false),
	NORD_DARK("Nord Dark", NordDark.class, true),
	CUPERTINO_LIGHT("Cupertino Light", CupertinoLight.class, false),
	CUPERTINO_DARK("Cupertino Dark", CupertinoDark.class, true),
	DRACULA("Dracula", Dracula.class, true);

	private final String name;
	private final Class<? extends Theme> themeClass;
	private final boolean isDark;

	AppTheme(String name, Class<? extends Theme> themeClass, boolean isDark)
	{
		this.name = name;
		this.themeClass = themeClass;
		this.isDark = isDark;
	}

	public String getName()
	{
		return name;
	}

	public Class<? extends Theme> getThemeClass()
	{
		return themeClass;
	}

	public boolean isDark()
	{
		return isDark;
	}

	public static AppTheme findByName(String name)
	{
		return Arrays.stream(values()).filter(appTheme -> appTheme.getName().equals(name)).findFirst().orElse(null);
	}

	@Override
	public String toString()
	{
		return name;
	}
}
