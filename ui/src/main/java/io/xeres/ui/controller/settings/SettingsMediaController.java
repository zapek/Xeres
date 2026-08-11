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

package io.xeres.ui.controller.settings;

import io.xeres.ui.model.settings.Settings;
import io.xeres.ui.support.preference.PreferenceUtils;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.stereotype.Component;

import java.util.prefs.Preferences;

import static io.xeres.ui.controller.messaging.MessagingWindowController.URL_PREVIEWS;
import static io.xeres.ui.support.preference.PreferenceUtils.CHATS;
import static io.xeres.ui.support.preference.PreferenceUtils.MISC;
import static io.xeres.ui.support.uri.UriService.EXTERNAL_URL_NO_WARNING;

@Component
@FxmlView(value = "/view/settings/settings_media.fxml")
public class SettingsMediaController implements SettingsController
{
	@FXML
	private CheckBox enableUrlPreview;

	@FXML
	private CheckBox enableExternalUrlWarnings;

	private Preferences chatsPreferences;
	private Preferences miscPreferences;

	@Override
	public void initialize()
	{
		var preferences = PreferenceUtils.getPreferences();
		chatsPreferences = preferences.node(CHATS);
		miscPreferences = preferences.node(MISC);
	}

	@Override
	public void onLoad(Settings settings)
	{
		enableUrlPreview.setSelected(chatsPreferences.getBoolean(URL_PREVIEWS, false));
		enableExternalUrlWarnings.setSelected(!miscPreferences.getBoolean(EXTERNAL_URL_NO_WARNING, false));
	}

	@Override
	public Settings onSave()
	{
		chatsPreferences.putBoolean(URL_PREVIEWS, enableUrlPreview.isSelected());
		miscPreferences.putBoolean(EXTERNAL_URL_NO_WARNING, !enableExternalUrlWarnings.isSelected());
		return null;
	}
}
