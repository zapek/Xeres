/*
 * Copyright (c) 2025-2026 by David Gerber - https://zapek.com
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

package io.xeres.ui.controller.board;

import atlantafx.base.controls.Tab;
import atlantafx.base.controls.TabLine;
import io.xeres.common.util.OsUtils;
import io.xeres.ui.client.BoardClient;
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
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import net.rgielen.fxweaver.core.FxmlView;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

import static io.xeres.ui.support.util.UiUtils.getWindow;

@Component
@FxmlView(value = "/view/board/board_editor_view.fxml")
public class BoardEditorWindowController implements WindowController
{
	private static final Logger log = LoggerFactory.getLogger(BoardEditorWindowController.class);

	@FXML
	private GridPane gridPane;

	@FXML
	private TextField boardName;

	@FXML
	private TextField title;

	@FXML
	private TabLine tabLine;

	@FXML
	private Tab textTab;

	@FXML
	private Tab imageTab;

	@FXML
	private Tab linkTab;

	@FXML
	private EditorView editorView;

	@FXML
	private Button send;

	private final List<Node> addedNodes = new ArrayList<>();

	private ImageSelectorView imageSelectorView;

	private Label linkLabel;

	private TextField linkTextField;

	private long boardId;
	private File imageFile;

	private final BoardClient boardClient;
	private final LocationClient locationClient;
	private final MarkdownService markdownService;
	private final ResourceBundle bundle;

	public BoardEditorWindowController(BoardClient boardClient, LocationClient locationClient, MarkdownService markdownService, ResourceBundle bundle)
	{
		this.boardClient = boardClient;
		this.locationClient = locationClient;
		this.markdownService = markdownService;
		this.bundle = bundle;
	}

	@Override
	public void initialize()
	{
		imageSelectorView = new ImageSelectorView(240.0, 180.0, "mdi2i-image", true);
		imageSelectorView.setOnSelectAction(this::selectMessageImage);
		imageSelectorView.setOnDeleteAction(this::clearMessageImage);
		linkLabel = new Label("URL");
		linkTextField = new TextField();

		tabLine.setTabClosingPolicy(Tab.ClosingPolicy.NO_TABS);
		tabLine.setTabDragPolicy(Tab.DragPolicy.FIXED);
		tabLine.setTabResizePolicy(Tab.ResizePolicy.ADAPTIVE);

		tabLine.getSelectionModel().selectedItemProperty().subscribe(tab -> {
			clearPanelContent();
			if (tab == imageTab)
			{
				gridPane.add(addPanelContent(imageSelectorView), 0, 3, 2, 1);
			}
			else if (tab == linkTab)
			{
				gridPane.add(addPanelContent(linkLabel), 0, 3);
				gridPane.add(addPanelContent(linkTextField), 1, 3);
			}
		});

		Platform.runLater(() -> title.requestFocus());

		editorView.setInputContextMenu(locationClient);
		editorView.setMarkdownService(markdownService);
		title.setOnKeyTyped(_ -> checkSendable());

		send.setOnAction(_ -> postMessage());
	}

	private Node addPanelContent(Node node)
	{
		addedNodes.add(node);
		return node;
	}

	private void clearPanelContent()
	{
		addedNodes.forEach(node -> gridPane.getChildren().remove(node));
		addedNodes.clear();
	}

	private void checkSendable()
	{
		send.setDisable(StringUtils.isBlank(title.getText()));
	}

	@Override
	public void onShown()
	{
		var userData = UiUtils.getUserData(title);
		if (userData == null)
		{
			throw new IllegalArgumentException("Missing board id");
		}

		boardId = (long) userData;

		boardClient.getBoardGroupById(boardId)
				.doOnSuccess(boardGroup -> Platform.runLater(() -> boardName.setText(boardGroup.getName())))
				.subscribe();

		// Prevent the message from being discarded by mistake
		UiUtils.getWindow(send).setOnCloseRequest(event -> {
			if (!title.getText().isBlank() || editorView.isModified() || !imageSelectorView.isEmpty() || !linkTextField.getText().isBlank())
			{
				UiUtils.alertConfirm(bundle.getString("board.editor.cancel"), () -> UiUtils.getWindow(send).hide());
				event.consume();
			}
		});
	}

	private void postMessage()
	{
		// XXX: add a spinner delay, then clear it on error, also display errors
		boardClient.createBoardMessage(boardId, title.getText(), editorView.getText(), linkTextField.getText(), imageFile, 0L)
				.doOnSuccess(_ -> Platform.runLater(() -> UiUtils.closeWindow(send)))
				.subscribe();
	}

	private void selectMessageImage(ActionEvent event)
	{
		var fileChooser = new FileChooser();
		fileChooser.setTitle(bundle.getString("board.select-image"));
		ChooserUtils.setInitialDirectory(fileChooser, OsUtils.getDownloadDir());
		ChooserUtils.setSupportedLoadImageFormats(fileChooser);
		var selectedFile = fileChooser.showOpenDialog(getWindow(event));
		if (selectedFile != null && selectedFile.canRead())
		{
			CompletableFuture.runAsync(() -> {
				try (var inputStream = new FileInputStream(selectedFile))
				{
					var image = new Image(inputStream);
					Platform.runLater(() -> {
						imageSelectorView.setImage(image);
						imageFile = selectedFile;
					});
				}
				catch (IOException e)
				{
					imageFile = null;
					UiUtils.alert(Alert.AlertType.ERROR, MessageFormat.format(bundle.getString("file-requester.error"), selectedFile, e.getMessage()));
				}
			});
		}
	}

	private void clearMessageImage(ActionEvent event)
	{
		imageFile = null;
		imageSelectorView.setImage(null);
	}
}
