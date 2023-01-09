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

package io.xeres.ui.controller.qrcode;

import io.xeres.ui.JavaFxApplication;
import io.xeres.ui.controller.WindowController;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static io.xeres.common.rest.PathConfig.LOCATIONS_PATH;

@Component
@FxmlView(value = "/view/qrcode/qrcode.fxml")
public class QrCodeWindowController implements WindowController
{
	@FXML
	private ImageView ownQrCode;

	@Override
	public void initialize() throws IOException
	{
		// Nothing to do
	}

	@Override
	public void onShown()
	{
		ownQrCode.setImage(new Image(JavaFxApplication.getControlUrl() + LOCATIONS_PATH + "/" + 1L + "/rsId/qrCode", true));
	}
}
