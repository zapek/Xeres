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

package io.xeres.ui.controller.id;

import io.xeres.common.geoip.Country;
import io.xeres.common.id.Id;
import io.xeres.common.pgp.Trust;
import io.xeres.common.protocol.HostPort;
import io.xeres.common.protocol.i2p.I2pAddress;
import io.xeres.common.protocol.ip.IP;
import io.xeres.common.protocol.tor.OnionAddress;
import io.xeres.ui.client.GeoIpClient;
import io.xeres.ui.client.ProfileClient;
import io.xeres.ui.controller.WindowController;
import io.xeres.ui.model.connection.Connection;
import io.xeres.ui.model.profile.Profile;
import io.xeres.ui.support.util.UiUtils;
import io.xeres.ui.support.window.WindowManager;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;
import net.rgielen.fxweaver.core.FxmlView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

@Component
@FxmlView(value = "/view/id/rsid_add.fxml")
public class AddRsIdWindowController implements WindowController
{
	private static final Logger log = LoggerFactory.getLogger(AddRsIdWindowController.class);

	@FXML
	private Button cancelButton;

	@FXML
	private Button addButton;

	@FXML
	private TextFlow instructions;

	@FXML
	private TextArea rsIdTextArea;

	@FXML
	private TextField certName;

	@FXML
	private TextField certId;

	@FXML
	private TextField certFingerprint;

	@FXML
	private TextField certLocId;

	@FXML
	private ComboBox<AddressCountry> certIps;

	@FXML
	private ChoiceBox<Trust> trust;

	@FXML
	private TitledPane titledPane;

	@FXML
	private Label status;

	@FXML
	private Hyperlink scanQrCode;

	private final ProfileClient profileClient;
	private final GeoIpClient geoIpClient;
	private final ResourceBundle bundle;
	private final WindowManager windowManager;

	private Profile ownProfile;

	public AddRsIdWindowController(ProfileClient profileClient, GeoIpClient geoIpClient, ResourceBundle bundle, WindowManager windowManager)
	{
		this.profileClient = profileClient;
		this.geoIpClient = geoIpClient;
		this.bundle = bundle;
		this.windowManager = windowManager;
	}

	@Override
	public void initialize()
	{
		scanQrCode.setOnAction(event -> windowManager.openCamera(UiUtils.getWindow(event), this));
		addButton.setOnAction(event -> addPeer());
		cancelButton.setOnAction(UiUtils::closeWindow);

		var debouncer = new PauseTransition(Duration.millis(250.0));
		rsIdTextArea.textProperty().addListener((observable, oldValue, newValue) -> {
			debouncer.setOnFinished(event -> checkRsId(newValue));
			debouncer.playFromStart();
		});

		profileClient.getOwn()
				.doOnSuccess(profile -> ownProfile = profile)
				.subscribe();

		certIps.setCellFactory(param -> new AddressCell());
		certIps.setConverter(new AddressConverter());

		Platform.runLater(this::handleArgument);
	}

	private void handleArgument()
	{
		var userData = rsIdTextArea.getScene().getRoot().getUserData();
		if (userData != null)
		{
			setRsId((String) userData);
			rsIdTextArea.setEditable(false);
			instructions.setVisible(false);
			instructions.setManaged(false);
		}
		else
		{
			rsIdTextArea.requestFocus();
		}
	}

	public void setRsId(String rsId)
	{
		rsIdTextArea.setText(rsId);
		addButton.requestFocus();
	}

	private void addPeer()
	{
		var profile = profileClient.create(rsIdTextArea.getText(), certIps.getSelectionModel().getSelectedIndex(), trust.getSelectionModel().getSelectedItem());

		profile.doOnSuccess(aVoid -> Platform.runLater(() -> UiUtils.closeWindow(cancelButton)))
				.doOnError(UiUtils::showAlertError)
				.subscribe();
	}

	private void checkRsId(String rsId)
	{
		profileClient.checkRsId(rsId.replaceAll("([\r\n\t])", ""))
				.doOnSuccess(profile -> Platform.runLater(() ->
				{
					if (profile.getId() == ownProfile.getId())
					{
						status.setText(bundle.getString("rsid.add.no-own"));
						addButton.setDisable(true);
						UiUtils.showError(rsIdTextArea, status);
						return;
					}
					status.setText("");
					addButton.setDisable(false);
					UiUtils.clearError(rsIdTextArea, status);

					certName.setText(profile.getName());
					certId.setText(Id.toString(profile.getPgpIdentifier()));
					certFingerprint.setText(profile.getProfileFingerprint().toString());

					certIps.getItems().clear();
					profile.getLocations().stream()
							.findFirst()
							.ifPresent(location ->
							{
								certLocId.setText(location.getLocationId().toString());

								// The same sorting is used in PeerConnectionJob/connectImmediately()
								var allIps = location.getConnections().stream()
										.sorted(Comparator.comparing(Connection::isExternal).reversed())
										.map(Connection::getAddress)
										.toList();

								certIps.getItems().addAll(allIps.stream()
										.map(s -> new AddressCountry(s, null))
										.toList());

								CompletableFuture.runAsync(() -> Platform.runLater(() -> findFlags(certIps)));
							});
					setDefaultTrust(trust);
					titledPane.setExpanded(true);
				}))
				.doOnError(throwable -> Platform.runLater(() ->
				{
					addButton.setDisable(true);
					if (rsIdTextArea.getText().isBlank())
					{
						status.setText("");
						UiUtils.clearError(rsIdTextArea, status);
					}
					else
					{
						status.setText(bundle.getString("rsid.add.invalid"));
						UiUtils.showError(rsIdTextArea, status);
					}
					titledPane.setExpanded(false);
				}))
				.subscribe();
	}

	private void setDefaultTrust(ChoiceBox<Trust> trust)
	{
		trust.getItems().clear();
		trust.getItems().addAll(Arrays.stream(Trust.values()).filter(t -> t != Trust.ULTIMATE).toList());
		trust.getSelectionModel().select(Trust.UNKNOWN);
	}

	private void findFlags(ComboBox<AddressCountry> certIps)
	{
		for (var i = 0; i < certIps.getItems().size(); i++)
		{
			var item = certIps.getItems().get(i);
			Country country;

			if (OnionAddress.isValidAddress(item.address()))
			{
				country = Country.TOR;
			}
			else if (I2pAddress.isValidAddress(item.address()))
			{
				country = Country.I2P;
			}
			else
			{
				var hostPort = HostPort.parse(item.address());

				if (IP.isLanIp(hostPort.host()))
				{
					country = Country.LAN;
				}
				else
				{
					country = findByGeoIp(hostPort.host());
				}
			}

			if (country != null)
			{
				certIps.getItems().set(i, new AddressCountry(item.address(), country));
			}
		}
		certIps.getSelectionModel().select(0);
	}

	private Country findByGeoIp(String ip)
	{
		var countryResponse = geoIpClient.getIsoCountry(ip).block();
		if (countryResponse != null)
		{
			try
			{
				return Country.valueOf(countryResponse.isoCountry().toUpperCase(Locale.ROOT));
			}
			catch (IllegalArgumentException e)
			{
				log.warn("Country not found for iso {}", countryResponse.isoCountry());
			}
		}
		return null;
	}
}
