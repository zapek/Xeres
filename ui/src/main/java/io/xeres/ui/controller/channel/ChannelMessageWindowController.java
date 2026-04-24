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
import io.xeres.ui.custom.EditorView;
import io.xeres.ui.custom.ImageSelectorView;
import io.xeres.ui.model.channel.ChannelFile;
import io.xeres.ui.model.channel.ChannelFile.State;
import io.xeres.ui.support.clipboard.ClipboardUtils;
import io.xeres.ui.support.markdown.MarkdownService;
import io.xeres.ui.support.uri.FileUri;
import io.xeres.ui.support.uri.UriFactory;
import io.xeres.ui.support.util.ChooserUtils;
import io.xeres.ui.support.util.UiUtils;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.TransferMode;
import javafx.stage.FileChooser;
import net.rgielen.fxweaver.core.FxmlView;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.SignalType;

import java.io.File;
import java.util.*;

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
	private TableView<ChannelFile> channelFileTableView;

	@FXML
	private TableColumn<ChannelFile, String> tableName;

	@FXML
	private TableColumn<ChannelFile, Long> tableSize;

	@FXML
	private TableColumn<ChannelFile, State> tableState;

	@FXML
	private TableColumn<ChannelFile, String> tableHash;

	@FXML
	private Button send;

	@FXML
	private Button addFile;

	@FXML
	private Button removeFile;

	@FXML
	private Button pasteLink;

	private long channelId;

	private final ChannelClient channelClient;
	private final LocationClient locationClient;
	private final MarkdownService markdownService;
	private final ShareClient shareClient;
	private final ResourceBundle bundle;

	private final Queue<File> filesToAdd = new ArrayDeque<>();

	private final ObservableList<ChannelFile> files = FXCollections.observableArrayList();

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

		removeFile.setOnAction(_ -> channelFileTableView.getItems().removeAll(channelFileTableView.getSelectionModel().getSelectedItems()));
		removeFile.disableProperty().bind(Bindings.isEmpty(channelFileTableView.getSelectionModel().getSelectedItems()));

		pasteLink.setOnAction(_ -> {
			var s = ClipboardUtils.getStringFromClipboard();
			if (StringUtils.isNotBlank(s))
			{
				String[] lines = s.split("\\R");
				Arrays.stream(lines).forEach(line -> {
					var uri = UriFactory.createUri(line);
					if (uri instanceof FileUri fileUri)
					{
						addUri(fileUri);
					}
				});
			}
			else
			{
				UiUtils.showAlert(Alert.AlertType.INFORMATION, "Clipboard doesn't contain file links.");
			}
		});

		send.setOnAction(_ -> postMessage());

		tableName.setCellValueFactory(new PropertyValueFactory<>("name"));
		tableSize.setCellFactory(_ -> new ChannelFileSizeCell());
		tableSize.setCellValueFactory(new PropertyValueFactory<>("size"));
		tableState.setCellValueFactory(new PropertyValueFactory<>("state"));
		tableHash.setCellValueFactory(new PropertyValueFactory<>("hash"));
		channelFileTableView.setRowFactory(_ -> new ChannelMessageRow());

		channelFileTableView.setOnDragOver(event -> {
			if (event.getDragboard().hasFiles())
			{
				event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
			}
			event.consume();
		});
		channelFileTableView.setOnDragDropped(event -> {
			var droppedFiles = event.getDragboard().getFiles();
			addFiles(droppedFiles);
			event.setDropCompleted(true);
			event.consume();
		});
		channelFileTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		channelFileTableView.setItems(files);
	}

	private void addFiles(List<File> files)
	{
		filesToAdd.addAll(CollectionUtils.emptyIfNull(files));
		addNextFile();
	}

	private void addUri(FileUri fileUri)
	{
		var channelFile = new ChannelFile(fileUri.name(), null, State.DONE, fileUri.size(), fileUri.hash().toString());
		if (files.contains(channelFile))
		{
			return; // Already present
		}
		files.add(channelFile);
	}

	private void addNextFile()
	{
		var file = filesToAdd.poll();
		if (file != null)
		{
			var channelFile = new ChannelFile(file.getName(), file.getPath(), State.HASHING, file.length(), null);
			if (files.contains(channelFile))
			{
				return; // Already present
			}
			files.add(channelFile);

			shareClient.createTemporaryShare(file.getAbsolutePath())
					.doOnSuccess(result -> Platform.runLater(() -> {
						assert result != null;
						channelFile.setHash(result.hash());
						channelFile.setState(State.DONE);
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
		channelClient.createChannelMessage(channelId, title.getText(), editorView.getText(), postLogo.getFile(), files, 0L)
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
