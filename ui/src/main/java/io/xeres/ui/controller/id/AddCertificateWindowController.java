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
import io.xeres.ui.client.ProfileClient;
import io.xeres.ui.controller.WindowController;
import io.xeres.ui.model.connection.Connection;
import io.xeres.ui.support.util.UiUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import net.rgielen.fxweaver.core.FxmlView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Comparator;

@Component
@FxmlView(value = "/view/id/certificate_add.fxml")
public class AddCertificateWindowController implements WindowController
{
	private static final Logger log = LoggerFactory.getLogger(AddCertificateWindowController.class);

	@FXML
	private Button cancelButton;

	@FXML
	private Button addButton;

	@FXML
	private TextArea certificateTextArea;

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
	private TitledPane titledPane;

	private final ProfileClient profileClient;

	public AddCertificateWindowController(ProfileClient profileClient)
	{
		this.profileClient = profileClient;
	}

	public void initialize()
	{
		addButton.setOnAction(event -> addFriend());
		cancelButton.setOnAction(UiUtils::closeWindow);
		certificateTextArea.textProperty().addListener((observable, oldValue, newValue) -> checkCertificate(newValue)); // XXX: add a debouncer for this
	}

	private void addFriend()
	{
		Mono<Void> profile = profileClient.createProfile(certificateTextArea.getText());

		profile.doOnSuccess(aVoid -> Platform.runLater(() -> UiUtils.closeWindow(cancelButton)))
				.doOnError(throwable -> log.error("Error: {}", throwable.getMessage()))
				.subscribe();
	}

	private void checkCertificate(String certificateString)
	{
		profileClient.checkCertificate(certificateString.replaceAll("([\r\n\t])", ""))
				.doOnSuccess(profile -> Platform.runLater(() ->
				{
					certificateTextArea.setTooltip(new Tooltip("ID is valid"));
					addButton.setDisable(false);
					UiUtils.clearError(certificateTextArea);

					certName.setText(profile.getName());
					certId.setText(Id.toString(profile.getPgpIdentifier()));
					certFingerprint.setText(profile.getProfileFingerprint().toString());
					profile.getLocations().stream()
							.findFirst()
							.ifPresent(location ->
							{
								// XXX: display the hostname if available!
								certLocId.setText(location.getLocationId().toString());

								var allIps = location.getConnections().stream()
										.sorted(Comparator.comparing(Connection::isExternal).reversed())
										.map(Connection::getAddress)
										.toList();

								certIps.getItems().addAll(allIps);
								certIps.getSelectionModel().select(0);
								certIps.setTooltip(new Tooltip("All addresses will be tried. You can preselect one as a hint."));
							});
					titledPane.setExpanded(true);
				}))
				.doOnError(throwable ->
				{
					certificateTextArea.setTooltip(new Tooltip(throwable.getMessage()));
					addButton.setDisable(true);
					if (certificateTextArea.getText().isBlank())
					{
						UiUtils.clearError(certificateTextArea);
					}
					else
					{
						UiUtils.showError(certificateTextArea);
					}
					titledPane.setExpanded(false);
				})
				.subscribe();
	}
}
