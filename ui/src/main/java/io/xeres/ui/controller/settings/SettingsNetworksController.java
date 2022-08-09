/*
 * Copyright (c) 2019-2022 by David Gerber - https://zapek.com
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

import io.xeres.ui.controller.Controller;
import io.xeres.ui.support.util.TextFieldUtils;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class SettingsNetworksController implements Controller
{
	@FXML
	private TextField torSocksHost;

	@FXML
	private TextField torSocksPort;

	@FXML
	private TextField i2pSocksHost;

	@FXML
	private TextField i2pSocksPort;

	@Override
	public void initialize()
	{
		TextFieldUtils.setHost(torSocksHost);
		TextFieldUtils.setNumeric(torSocksPort, 0, 65535);

		TextFieldUtils.setHost(i2pSocksHost);
		TextFieldUtils.setNumeric(i2pSocksPort, 0, 65535);
	}
}
