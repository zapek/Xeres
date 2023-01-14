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

import io.xeres.ui.controller.Controller;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;

import java.io.IOException;

public class QrPrintController implements Controller
{
	@FXML
	private ImageView qrCode;

	@FXML
	private Text profileText;

	@FXML
	private Text locationText;

	@Override
	public void initialize() throws IOException
	{
		// Nothing to do
	}

	public void setImage(Image image)
	{
		qrCode.setImage(image);
	}

	public void setProfileName(String name)
	{
		profileText.setText(name);
	}

	public void setLocationName(String name)
	{
		locationText.setText(name);
	}
}
