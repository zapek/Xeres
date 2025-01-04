/*
 * Copyright (c) 2019-2025 by David Gerber - https://zapek.com
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

import io.xeres.ui.client.ConfigClient;
import io.xeres.ui.model.settings.Settings;
import io.xeres.ui.support.util.TextFieldUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.stereotype.Component;

@Component
@FxmlView(value = "/view/settings/settings_networks.fxml")
public class SettingsNetworksController implements SettingsController
{
	@FXML
	private TextField torSocksHost;

	@FXML
	private TextField torSocksPort;

	@FXML
	private TextField i2pSocksHost;

	@FXML
	private TextField i2pSocksPort;

	@FXML
	private CheckBox upnpEnabled;

	@FXML
	private TextField externalIp;

	@FXML
	private TextField externalPort;

	@FXML
	private CheckBox broadcastDiscoveryEnabled;

	@FXML
	private TextField internalIp;

	@FXML
	private TextField internalPort;

	@FXML
	private CheckBox dhtEnabled;

	private Settings settings;

	private final ConfigClient configClient;

	public SettingsNetworksController(ConfigClient configClient)
	{
		this.configClient = configClient;
	}

	@Override
	public void initialize()
	{
		TextFieldUtils.setHost(torSocksHost);
		TextFieldUtils.setNumeric(torSocksPort, 0, 6);

		TextFieldUtils.setHost(i2pSocksHost);
		TextFieldUtils.setNumeric(i2pSocksPort, 0, 6);

		configClient.getExternalIpAddress()
				.doOnSuccess(ipAddressResponse -> Platform.runLater(() -> {
					externalIp.setText(ipAddressResponse.ip());
					externalPort.setText(String.valueOf(ipAddressResponse.port()));
				}))
				.subscribe();

		configClient.getInternalIpAddress()
				.doOnSuccess(ipAddressResponse -> Platform.runLater(() -> {
					internalIp.setText(ipAddressResponse.ip());
					internalPort.setText(String.valueOf(ipAddressResponse.port()));
				}))
				.subscribe();
	}

	@Override
	public void onLoad(Settings settings)
	{
		this.settings = settings;

		torSocksHost.setText(settings.getTorSocksHost());
		if (settings.getTorSocksPort() != 0)
		{
			torSocksPort.setText(String.valueOf(settings.getTorSocksPort()));
		}

		i2pSocksHost.setText(settings.getI2pSocksHost());
		if (settings.getI2pSocksPort() != 0)
		{
			i2pSocksPort.setText(String.valueOf(settings.getI2pSocksPort()));
		}

		upnpEnabled.setSelected(settings.isUpnpEnabled());

		broadcastDiscoveryEnabled.setSelected(settings.isBroadcastDiscoveryEnabled());

		dhtEnabled.setSelected(settings.isDhtEnabled());
	}

	@Override
	public Settings onSave()
	{
		settings.setTorSocksHost(TextFieldUtils.getString(torSocksHost));
		settings.setTorSocksPort(limitPort(TextFieldUtils.getAsNumber(torSocksPort)));

		settings.setI2pSocksHost(TextFieldUtils.getString(i2pSocksHost));
		settings.setI2pSocksPort(limitPort(TextFieldUtils.getAsNumber(i2pSocksPort)));

		settings.setUpnpEnabled(upnpEnabled.isSelected());

		settings.setBroadcastDiscoveryEnabled(broadcastDiscoveryEnabled.isSelected());

		settings.setDhtEnabled(dhtEnabled.isSelected());

		return settings;
	}

	private int limitPort(int port)
	{
		if (port > 65535)
		{
			port = 65535;
		}
		return port;
	}
}
