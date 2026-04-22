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
import io.xeres.ui.client.ShareClient;
import io.xeres.ui.controller.WindowController;
import io.xeres.ui.controller.channel.FileAttachment.State;
import io.xeres.ui.custom.EditorView;
import io.xeres.ui.custom.ImageSelectorView;
import io.xeres.ui.support.markdown.MarkdownService;
import io.xeres.ui.support.util.ChooserUtils;
import io.xeres.ui.support.util.UiUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.TransferMode;
import javafx.stage.FileChooser;
import net.rgielen.fxweaver.core.FxmlView;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.SignalType;

import java.io.File;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
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
	private TableView<FileAttachment> attachmentTableView;

	@FXML
	private TableColumn<FileAttachment, String> tableName;

	@FXML
	private TableColumn<FileAttachment, Long> tableSize;

	@FXML
	private TableColumn<FileAttachment, State> tableState;

	@FXML
	private TableColumn<FileAttachment, String> tableHash;

	@FXML
	private Button send;

	@FXML
	private Button addFile;

	private long channelId;

	private final ChannelClient channelClient;
	private final LocationClient locationClient;
	private final MarkdownService markdownService;
	private final ShareClient shareClient;
	private final ResourceBundle bundle;

	private final Queue<File> filesToAdd = new ArrayDeque<>();

	private final ObservableList<FileAttachment> files = FXCollections.observableArrayList();

	public ChannelMessageWindowController(ChannelClient channelClient, LocationClient locationClient, MarkdownService markdownService, ShareClient shareClient, ResourceBundle bundle)
	{
		this.channelClient = channelClient;
		this.locationClient = locationClient;
		this.markdownService = markdownService;
		this.shareClient = shareClient;
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

		addFile.setOnAction(event -> {
			var fileChooser = new FileChooser();
			fileChooser.setTitle("Select file(s) to add");
			var selectedFiles = fileChooser.showOpenMultipleDialog(getWindow(event));
			if (selectedFiles != null)
			{
				addFiles(selectedFiles);
			}
		});
		send.setOnAction(_ -> postMessage());

		tableName.setCellValueFactory(new PropertyValueFactory<>("name"));
		tableSize.setCellFactory(_ -> new FileAttachmentSizeCell());
		tableSize.setCellValueFactory(new PropertyValueFactory<>("size"));
		tableState.setCellValueFactory(new PropertyValueFactory<>("state"));
		tableHash.setCellValueFactory(new PropertyValueFactory<>("hash"));

		attachmentTableView.setOnDragOver(event -> {
			if (event.getDragboard().hasFiles())
			{
				event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
			}
			event.consume();
		});
		attachmentTableView.setOnDragDropped(event -> {
			var droppedFiles = event.getDragboard().getFiles();
			addFiles(droppedFiles);
			event.setDropCompleted(true);
			event.consume();
		});
		attachmentTableView.setItems(files);
	}

	private void addFiles(List<File> files)
	{
		filesToAdd.addAll(CollectionUtils.emptyIfNull(files));
		addNextFile();
	}

	private void addFile(File file)
	{
		filesToAdd.add(file);
		addNextFile();
	}

	private void addNextFile()
	{
		var file = filesToAdd.poll();
		if (file != null)
		{
			var fileAttachment = new FileAttachment(file.getName(), file.getPath(), State.HASHING, file.length(), null);
			files.add(fileAttachment);

			shareClient.createTemporaryShare(file.getAbsolutePath())
					.doOnSuccess(result -> Platform.runLater(() -> {
						assert result != null;
						fileAttachment.setHash(result.hash());
						fileAttachment.setState(State.DONE);
					}))
					.doFinally(signalType -> {
						if (signalType != SignalType.CANCEL)
						{
							Platform.runLater(this::addNextFile);
						}
					})
					.subscribe();
		}
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
		// XXX: add a spinner delay, then clear it on error
		channelClient.createChannelMessage(channelId, title.getText(), editorView.getText(), postLogo.getFile(), 0L)
				.doOnSuccess(_ -> Platform.runLater(() -> UiUtils.closeWindow(send)))
				.doOnError(UiUtils::webAlertError)
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
