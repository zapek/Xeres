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

import io.xeres.ui.support.preference.PreferenceService;
import io.xeres.ui.support.window.UiBorders;
import javafx.application.Application;
import javafx.application.ColorScheme;
import javafx.application.Platform;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

@Component
public class AppThemeManager
{
	public static final String NODE_APPLICATION = "Application";
	public static final String KEY_THEME = "Theme";

	private final AppTheme defaultTheme;

	private final PreferenceService preferenceService;

	public AppThemeManager(PreferenceService preferenceService)
	{
		this.preferenceService = preferenceService;
		defaultTheme = getDefaultTheme();
	}

	public AppTheme getCurrentTheme()
	{
		var rootPreferences = preferenceService.getPreferences();
		if (rootPreferences == null)
		{
			return defaultTheme;
		}
		var preferences = rootPreferences.node(NODE_APPLICATION);
		return Optional.ofNullable(AppTheme.findByName(preferences.get(KEY_THEME, String.valueOf(defaultTheme)))).orElse(defaultTheme);
	}

	public void applyCurrentTheme()
	{
		applyTheme(getCurrentTheme());
	}

	public void changeTheme(AppTheme appTheme)
	{
		applyTheme(appTheme);
		UiBorders.setDarkModeAll(appTheme.isDark());
		saveCurrentTheme(appTheme);
	}

	private AppTheme getDefaultTheme()
	{
		return switch (Platform.getPreferences().getColorScheme())
		{
			case ColorScheme.LIGHT -> AppTheme.PRIMER_LIGHT;
			case ColorScheme.DARK -> AppTheme.PRIMER_DARK;
		};
	}

	private void applyTheme(AppTheme appTheme)
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

	private void saveCurrentTheme(AppTheme appTheme)
	{
		var preferences = preferenceService.getPreferences().node(NODE_APPLICATION);
		preferences.put(KEY_THEME, appTheme.getName());
	}
}
