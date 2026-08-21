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

package io.xeres.ui.custom.sticker;

import io.xeres.common.i18n.I18nUtils;
import io.xeres.common.util.OsUtils;
import io.xeres.ui.custom.DisclosedHyperlink;
import io.xeres.ui.support.util.UiUtils;
import io.xeres.ui.support.window.WindowManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tab;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ResourceBundle;

import static io.xeres.ui.controller.help.HelpWindowController.SECTION_MISC_STICKERS;
import static io.xeres.ui.custom.sticker.StickerView.IMAGE_MAIN_HEIGHT;

public class EmptyStickerTab extends Tab
{
	private static final ResourceBundle bundle = I18nUtils.getBundle();

	@FXML
	private FontIcon icon;

	@FXML
	private DisclosedHyperlink stickersDirectory;

	@FXML
	private DisclosedHyperlink helpLink;

	private final Path userPath;

	public EmptyStickerTab(Path userPath)
	{
		this.userPath = userPath;

		var loader = new FXMLLoader(EmptyStickerTab.class.getResource("/view/custom/sticker_empty.fxml"), bundle);
		loader.setRoot(this);
		loader.setController(this);

		try
		{
			loader.load();
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	@FXML
	private void initialize()
	{
		stickersDirectory.setOnAction(_ -> OsUtils.showFolder(userPath.toFile()));
		helpLink.setOnAction(_ -> WindowManager.openHelpStatic(SECTION_MISC_STICKERS));
		UiUtils.setIconSize(icon, IMAGE_MAIN_HEIGHT);
	}
}
