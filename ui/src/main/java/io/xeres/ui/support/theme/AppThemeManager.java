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

import io.xeres.ui.support.window.UiBorders;
import javafx.application.Application;

import java.lang.reflect.InvocationTargetException;
import java.util.prefs.Preferences;

public final class AppThemeManager
{
	private AppThemeManager()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static AppTheme getCurrentTheme()
	{
		var preferences = Preferences.userRoot().node("Application");
		return AppTheme.findByName(preferences.get("Theme", String.valueOf(AppTheme.PRIMER_LIGHT)));
	}

	public static void applyCurrentTheme()
	{
		applyTheme(getCurrentTheme());
	}

	public static void changeTheme(AppTheme appTheme)
	{
		applyTheme(appTheme);
		UiBorders.setDarkModeAll(appTheme.isDark());
		saveCurrentTheme(appTheme);
	}

	private static void applyTheme(AppTheme appTheme)
	{
		try
		{
			Application.setUserAgentStylesheet(appTheme.getThemeClass().getDeclaredConstructor().newInstance().getUserAgentStylesheet());
		}
		catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e)
		{
			throw new RuntimeException(e);
		}
	}

	private static void saveCurrentTheme(AppTheme appTheme)
	{
		var preferences = Preferences.userRoot().node("Application");
		preferences.put("Theme", appTheme.getName());
	}
}
