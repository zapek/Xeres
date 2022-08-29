/*
 * Copyright (c) 2019-2021 by David Gerber - https://zapek.com
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

import io.xeres.common.id.Id;
import io.xeres.common.pgp.Trust;
import io.xeres.ui.client.ProfileClient;
import io.xeres.ui.controller.WindowController;
import io.xeres.ui.model.connection.Connection;
import io.xeres.ui.model.profile.Profile;
import io.xeres.ui.support.util.UiUtils;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.Duration;
import net.rgielen.fxweaver.core.FxmlView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Comparator;

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
	private ChoiceBox<String> certIps;

	@FXML
	private ChoiceBox<Trust> trust;

	@FXML
	private TitledPane titledPane;

	@FXML
	private Label status;

	private final ProfileClient profileClient;

	private Profile ownProfile;

	public AddRsIdWindowController(ProfileClient profileClient)
	{
		this.profileClient = profileClient;
	}

	@Override
	public void initialize()
	{
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

		Platform.runLater(this::handleArgument);
	}

	private void handleArgument()
	{
		var userData = rsIdTextArea.getScene().getRoot().getUserData();
		if (userData != null)
		{
			rsIdTextArea.setText((String) userData);
		}
	}

	private void addPeer()
	{
		var profile = profileClient.create(rsIdTextArea.getText(), certIps.getSelectionModel().getSelectedIndex(), trust.getSelectionModel().getSelectedItem());

		profile.doOnSuccess(aVoid -> Platform.runLater(() -> UiUtils.closeWindow(cancelButton)))
				.doOnError(throwable -> log.error("Error: {}", throwable.getMessage()))
				.subscribe();
	}

	private void checkRsId(String rsId)
	{
		profileClient.checkRsId(rsId.replaceAll("([\r\n\t])", ""))
				.doOnSuccess(profile -> Platform.runLater(() ->
				{
					if (profile.getId() == ownProfile.getId())
					{
						status.setText("You can't add your own ID");
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
								// XXX: display the hostname if available!
								certLocId.setText(location.getLocationId().toString());

								// The same sorting is used in PeerConnectionJob/connectImmediately()
								var allIps = location.getConnections().stream()
										.sorted(Comparator.comparing(Connection::isExternal).reversed())
										.map(Connection::getAddress)
										.toList();

								certIps.getItems().addAll(allIps);
								certIps.getSelectionModel().select(0);
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
						status.setText("Invalid ID");
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
}
