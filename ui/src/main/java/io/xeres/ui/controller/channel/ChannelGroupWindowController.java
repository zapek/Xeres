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

package io.xeres.ui.controller.channel;

import io.xeres.common.util.OsUtils;
import io.xeres.common.util.RemoteUtils;
import io.xeres.ui.client.ChannelClient;
import io.xeres.ui.client.GeneralClient;
import io.xeres.ui.controller.WindowController;
import io.xeres.ui.custom.ImageSelectorView;
import io.xeres.ui.support.util.ChooserUtils;
import io.xeres.ui.support.util.UiUtils;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import net.rgielen.fxweaver.core.FxmlView;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Component;

import java.util.ResourceBundle;

import static io.xeres.common.rest.PathConfig.CHANNELS_PATH;
import static io.xeres.ui.support.util.UiUtils.getWindow;

@Component
@FxmlView(value = "/view/channel/channel_group_view.fxml")
public class ChannelGroupWindowController implements WindowController
{
	@FXML
	private Button createOrUpdateButton;

	@FXML
	private Button cancelButton;

	@FXML
	private TextField channelName;

	@FXML
	private TextField channelDescription;

	@FXML
	private ImageSelectorView channelLogo;

	@FXML
	private ProgressBar progressBar;

	private final ChannelClient channelClient;
	private final GeneralClient generalClient;

	private final ResourceBundle bundle;

	private long channelId;

	private String initialUrl;
	private String initialName;
	private String initialDescription;

	public ChannelGroupWindowController(ChannelClient channelClient, GeneralClient generalClient, ResourceBundle bundle)
	{
		this.channelClient = channelClient;
		this.generalClient = generalClient;
		this.bundle = bundle;
	}

	@Override
	public void initialize()
	{
		channelName.textProperty().addListener(_ -> checkCreatableOrUpdatable());
		channelDescription.textProperty().addListener(_ -> checkCreatableOrUpdatable());
		channelLogo.imageProperty().addListener(_ -> checkCreatableOrUpdatable());
		channelLogo.setOnSelectAction(this::selectGroupImage);
		channelLogo.setOnDeleteAction(this::clearGroupImage);
		channelLogo.setImageLoader(url -> generalClient.getImage(url).block());

		cancelButton.setOnAction(UiUtils::closeWindow);
	}

	@Override
	public void onShown()
	{
		var userData = UiUtils.getUserData(channelName);
		if (userData != null)
		{
			channelId = (long) userData;
		}

		if (channelId != 0L)
		{
			channelClient.getChannelGroupById(channelId)
					.doOnSuccess(channelGroup -> Platform.runLater(() -> {
						assert channelGroup != null;
						channelName.setText(channelGroup.getName());
						channelDescription.setText(channelGroup.getDescription());
						if (channelGroup.hasImage())
						{
							channelLogo.setImageUrl(RemoteUtils.getControlUrl() + CHANNELS_PATH + "/groups/" + channelGroup.getId() + "/image");
							initialUrl = channelLogo.getUrl();
						}
						initialName = channelName.getText();
						initialDescription = channelDescription.getText();
						createOrUpdateButton.setDisable(true);
					}))
					.subscribe();
			createOrUpdateButton.setText("Update");
			createOrUpdateButton.setOnAction(_ -> {
				setWaiting(true);
				channelClient.updateChannelGroup(channelId,
								channelName.getText(),
								channelDescription.getText(),
								channelLogo.getFile(),
								!Strings.CS.equals(initialUrl, channelLogo.getUrl()))
						.doOnSuccess(_ -> Platform.runLater(() -> UiUtils.closeWindow(channelName)))
						.doFinally(_ -> setWaiting(false))
						.subscribe();
			});
		}
		else
		{
			createOrUpdateButton.setOnAction(_ -> {
				setWaiting(true);
				channelClient.createChannelGroup(channelName.getText(),
								channelDescription.getText(),
								channelLogo.getFile())
						.doOnSuccess(_ -> Platform.runLater(() -> UiUtils.closeWindow(channelName)))
						.doFinally(_ -> setWaiting(false))
						.subscribe();
			});
		}
	}

	private void setWaiting(boolean waiting)
	{
		channelName.setDisable(waiting);
		channelDescription.setDisable(waiting);
		channelLogo.setDisable(waiting);
		createOrUpdateButton.setDisable(waiting);
		cancelButton.setDisable(waiting);
		progressBar.setVisible(waiting);
	}

	private void checkCreatableOrUpdatable()
	{
		createOrUpdateButton.setDisable((channelId == 0L && channelName.getText().isBlank()) ||
				(channelId == 0L && channelDescription.getText().isBlank()) ||
				(
						Strings.CS.equals(initialName, channelName.getText()) &&
								Strings.CS.equals(initialDescription, channelDescription.getText()) &&
								Strings.CS.equals(initialUrl, channelLogo.getUrl())
				)
		);
	}

	private void selectGroupImage(ActionEvent event)
	{
		var fileChooser = new FileChooser();
		fileChooser.setTitle(bundle.getString("channel.select-logo"));
		ChooserUtils.setInitialDirectory(fileChooser, OsUtils.getDownloadDir());
		ChooserUtils.setSupportedLoadImageFormats(fileChooser);
		var selectedFile = fileChooser.showOpenDialog(getWindow(event));
		channelLogo.setFile(selectedFile);
	}

	private void clearGroupImage(ActionEvent event)
	{
		channelLogo.setImage(null);
	}
}
