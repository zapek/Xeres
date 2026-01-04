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

import io.xeres.common.util.OsUtils;
import io.xeres.common.util.RemoteUtils;
import io.xeres.ui.client.BoardClient;
import io.xeres.ui.client.GeneralClient;
import io.xeres.ui.controller.WindowController;
import io.xeres.ui.custom.ImageSelectorView;
import io.xeres.ui.support.util.ChooserUtils;
import io.xeres.ui.support.util.UiUtils;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

import static io.xeres.common.rest.PathConfig.BOARDS_PATH;
import static io.xeres.ui.support.util.UiUtils.getWindow;

@Component
@FxmlView(value = "/view/board/board_group_view.fxml")
public class BoardGroupWindowController implements WindowController
{
	@FXML
	private Button createButton;

	@FXML
	private Button cancelButton;

	@FXML
	private TextField boardName;

	@FXML
	private TextField boardDescription;

	@FXML
	private ImageSelectorView boardLogo;

	private final BoardClient boardClient;
	private final GeneralClient generalClient;

	private final ResourceBundle bundle;

	private long boardId;

	private File logoFile;

	public BoardGroupWindowController(BoardClient boardClient, GeneralClient generalClient, ResourceBundle bundle)
	{
		this.boardClient = boardClient;
		this.generalClient = generalClient;
		this.bundle = bundle;
	}

	@Override
	public void initialize()
	{
		boardName.textProperty().addListener(_ -> checkCreatable());
		boardDescription.textProperty().addListener(_ -> checkCreatable());
		boardLogo.setOnSelectAction(this::selectGroupImage);
		boardLogo.setOnDeleteAction(this::clearGroupImage);
		boardLogo.setImageLoader(url -> generalClient.getImage(url).block());

		cancelButton.setOnAction(UiUtils::closeWindow);
	}

	@Override
	public void onShown()
	{
		var userData = UiUtils.getUserData(boardName);
		if (userData != null)
		{
			boardId = (long) userData;
		}

		if (boardId != 0L)
		{
			boardClient.getBoardGroupById(boardId)
					.doOnSuccess(boardGroup -> Platform.runLater(() -> {
						boardName.setText(boardGroup.getName());
						boardDescription.setText(boardGroup.getDescription());
						if (boardGroup.hasImage())
						{
							boardLogo.setImageUrl(RemoteUtils.getControlUrl() + BOARDS_PATH + "/groups/" + boardGroup.getId() + "/image");
						}
					}))
					.subscribe();
			createButton.setText("Update");
			createButton.setOnAction(_ -> boardClient.updateBoardGroup(boardId,
							boardName.getText(),
							boardDescription.getText(),
							logoFile,
							logoFile != null) // XXX: it depends on the initial state (set an image listener? how do I detect the change?). also checkCreateable() should be checkEdited() or so...
					.doOnSuccess(_ -> Platform.runLater(() -> UiUtils.closeWindow(boardName)))
					.subscribe());
		}
		else
		{
			createButton.setOnAction(_ -> boardClient.createBoardGroup(boardName.getText(),
							boardDescription.getText(),
							logoFile)
					.doOnSuccess(_ -> Platform.runLater(() -> UiUtils.closeWindow(boardName)))
					.subscribe());
		}
	}

	private void checkCreatable()
	{
		createButton.setDisable(boardName.getText().isBlank() || boardDescription.getText().isBlank());
	}

	private void selectGroupImage(ActionEvent event)
	{
		var fileChooser = new FileChooser();
		fileChooser.setTitle(bundle.getString("board.select-logo"));
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
						boardLogo.setImage(image);
						logoFile = selectedFile;
					});
				}
				catch (IOException e)
				{
					logoFile = null;
					UiUtils.alert(Alert.AlertType.ERROR, MessageFormat.format(bundle.getString("file-requester.error"), selectedFile, e.getMessage()));
				}
			});
		}
	}

	private void clearGroupImage(ActionEvent event)
	{
		logoFile = null;
		boardLogo.setImage(null);
	}
}
