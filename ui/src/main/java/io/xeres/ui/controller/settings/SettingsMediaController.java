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

import java.io.IOException;
import java.util.prefs.Preferences;

import static io.xeres.ui.controller.messaging.MessagingWindowController.URL_PREVIEWS;
import static io.xeres.ui.support.preference.PreferenceUtils.CHATS;

@Component
@FxmlView(value = "/view/settings/settings_media.fxml")
public class SettingsMediaController implements SettingsController
{
	@FXML
	private CheckBox enableUrlPreview;

	private Preferences preferences;

	@Override
	public void initialize() throws IOException
	{
		preferences = PreferenceUtils.getPreferences().node(CHATS);
	}

	@Override
	public void onLoad(Settings settings)
	{
		enableUrlPreview.setSelected(preferences.getBoolean(URL_PREVIEWS, false));
	}

	@Override
	public Settings onSave()
	{
		preferences.putBoolean(URL_PREVIEWS, enableUrlPreview.isSelected());
		return null;
	}
}
