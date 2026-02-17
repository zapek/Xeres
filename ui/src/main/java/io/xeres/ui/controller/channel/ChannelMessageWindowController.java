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
import io.xeres.ui.client.ChannelClient;
import io.xeres.ui.client.LocationClient;
import io.xeres.ui.controller.WindowController;
import io.xeres.ui.custom.EditorView;
import io.xeres.ui.custom.ImageSelectorView;
import io.xeres.ui.support.markdown.MarkdownService;
import io.xeres.ui.support.util.ChooserUtils;
import io.xeres.ui.support.util.UiUtils;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import net.rgielen.fxweaver.core.FxmlView;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ResourceBundle;

import static io.xeres.ui.support.util.UiUtils.getWindow;

@Component
@FxmlView(value = "/view/channel/channel_message_view.fxml")
public class ChannelMessageWindowController implements WindowController
{
	@FXML
	private TextField channelName;

	@FXML
	private TextField title;

	@FXML
	private ImageSelectorView postLogo;

	@FXML
	private EditorView editorView;

	@FXML
	private Button send;

	private long channelId;

	private final ChannelClient channelClient;
	private final LocationClient locationClient;
	private final MarkdownService markdownService;
	private final ResourceBundle bundle;

	public ChannelMessageWindowController(ChannelClient channelClient, LocationClient locationClient, MarkdownService markdownService, ResourceBundle bundle)
	{
		this.channelClient = channelClient;
		this.locationClient = locationClient;
		this.markdownService = markdownService;
		this.bundle = bundle;
	}

	@Override
	public void initialize()
	{
		postLogo.setOnSelectAction(this::selectMessageImage);
		postLogo.setOnDeleteAction(this::clearMessageImage);

		Platform.runLater(() -> title.requestFocus());

		editorView.setInputContextMenu(locationClient);
		editorView.setMarkdownService(markdownService);
		title.setOnKeyTyped(_ -> checkSendable());

		send.setOnAction(_ -> postMessage());
	}

	@Override
	public void onShown()
	{
		var userData = UiUtils.getUserData(title);
		if (userData == null)
		{
			throw new IllegalArgumentException("Missing channel id");
		}

		channelId = (long) userData;

		channelClient.getChannelGroupById(channelId)
				.doOnSuccess(channelGroup -> Platform.runLater(() -> {
					assert channelGroup != null;
					channelName.setText(channelGroup.getName());
				}))
				.subscribe();

		// Prevent the message from being discarded by mistake
		UiUtils.getWindow(send).setOnCloseRequest(event -> {
			if (!title.getText().isBlank() || editorView.isModified() || !postLogo.isEmpty()) // XXX: add file list condition
			{
				UiUtils.showAlertConfirm(bundle.getString("channel.editor.cancel"), () -> UiUtils.getWindow(send).hide());
				event.consume();
			}
		});
	}

	private void checkSendable()
	{
		send.setDisable(StringUtils.isBlank(title.getText())); // XXX: more?
	}

	private void postMessage()
	{
		// XXX: add a spinner delay, then clear it on error, also display errors
		channelClient.createChannelMessage(channelId, title.getText(), editorView.getText(), postLogo.getFile(), 0L)
				.doOnSuccess(_ -> Platform.runLater(() -> UiUtils.closeWindow(send)))
				.subscribe();
	}

	private void selectMessageImage(ActionEvent event)
	{
		var fileChooser = new FileChooser();
		fileChooser.setTitle(bundle.getString("channel.select-image"));
		ChooserUtils.setInitialDirectory(fileChooser, OsUtils.getDownloadDir());
		ChooserUtils.setSupportedLoadImageFormats(fileChooser);
		var selectedFile = fileChooser.showOpenDialog(getWindow(event));
		postLogo.setFile(selectedFile);
	}

	private void clearMessageImage(ActionEvent event)
	{
		postLogo.setImage(null);
	}
}
