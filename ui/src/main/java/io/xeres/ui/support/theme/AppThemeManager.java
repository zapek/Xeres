/*
 * Copyright (c) 2023-2025 by David Gerber - https://zapek.com
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

import io.xeres.common.properties.StartupProperties;
import io.xeres.ui.support.preference.PreferenceUtils;
import io.xeres.ui.support.window.UiBorders;
import javafx.application.Application;
import javafx.application.ColorScheme;
import javafx.application.Platform;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import java.util.prefs.Preferences;

import static io.xeres.common.properties.StartupProperties.Property.UI;

@Component
public class AppThemeManager
{
	public static final String NODE_APPLICATION = "Application";
	public static final String KEY_THEME = "Theme";

	private AppTheme defaultTheme;

	public AppTheme getCurrentTheme()
	{
		if (defaultTheme == null)
		{
			defaultTheme = getDefaultTheme();
		}

		Preferences rootPreferences;
		try
		{
			rootPreferences = PreferenceUtils.getPreferences();
		}
		catch (IllegalStateException _)
		{
			// This can be called when the preferences aren't fully setup (no LocationIdentifier known yet)
			// so in that case we simply use the default theme.
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

	private static AppTheme getDefaultTheme()
	{
		// If we start without a UI, the toolkit won't run,
		// and we can't use getPreferences().
		if (!StartupProperties.getBoolean(UI, true))
		{
			return AppTheme.PRIMER_LIGHT;
		}
		return switch (Platform.getPreferences().getColorScheme())
		{
			case ColorScheme.LIGHT -> AppTheme.PRIMER_LIGHT;
			case ColorScheme.DARK -> AppTheme.DRACULA;
		};
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

	private void saveCurrentTheme(AppTheme appTheme)
	{
		var preferences = PreferenceUtils.getPreferences().node(NODE_APPLICATION);
		preferences.put(KEY_THEME, appTheme.getName());
	}
}
