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
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import net.rgielen.fxweaver.core.FxmlView;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Component;

import java.util.ResourceBundle;

import static io.xeres.common.rest.PathConfig.BOARDS_PATH;
import static io.xeres.ui.support.util.UiUtils.getWindow;

@Component
@FxmlView(value = "/view/board/board_group_view.fxml")
public class BoardGroupWindowController implements WindowController
{
	@FXML
	private Button createOrUpdateButton;

	@FXML
	private Button cancelButton;

	@FXML
	private TextField boardName;

	@FXML
	private TextField boardDescription;

	@FXML
	private ImageSelectorView boardLogo;

	@FXML
	private ProgressBar progressBar;

	private final BoardClient boardClient;
	private final GeneralClient generalClient;

	private final ResourceBundle bundle;

	private long boardId;

	private String initialUrl;
	private String initialName;
	private String initialDescription;

	public BoardGroupWindowController(BoardClient boardClient, GeneralClient generalClient, ResourceBundle bundle)
	{
		this.boardClient = boardClient;
		this.generalClient = generalClient;
		this.bundle = bundle;
	}

	@Override
	public void initialize()
	{
		boardName.textProperty().addListener(_ -> checkCreatableOrUpdatable());
		boardDescription.textProperty().addListener(_ -> checkCreatableOrUpdatable());
		boardLogo.imageProperty().addListener(_ -> checkCreatableOrUpdatable());
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
							initialUrl = boardLogo.getUrl();
						}
						initialName = boardName.getText();
						initialDescription = boardDescription.getText();
						createOrUpdateButton.setDisable(true);
					}))
					.subscribe();
			createOrUpdateButton.setText("Update");
			createOrUpdateButton.setOnAction(_ -> {
				setWaiting(true);
				boardClient.updateBoardGroup(boardId,
								boardName.getText(),
								boardDescription.getText(),
								boardLogo.getFile(),
								!Strings.CS.equals(initialUrl, boardLogo.getUrl()))
						.doOnSuccess(_ -> Platform.runLater(() -> UiUtils.closeWindow(boardName)))
						.doFinally(_ -> setWaiting(false))
						.subscribe();
			});
		}
		else
		{
			createOrUpdateButton.setOnAction(_ -> {
				setWaiting(true);
				boardClient.createBoardGroup(boardName.getText(),
								boardDescription.getText(),
								boardLogo.getFile())
						.doOnSuccess(_ -> Platform.runLater(() -> UiUtils.closeWindow(boardName)))
						.doFinally(_ -> setWaiting(false))
						.subscribe();
			});
		}
	}

	private void setWaiting(boolean waiting)
	{
		boardName.setDisable(waiting);
		boardDescription.setDisable(waiting);
		boardLogo.setDisable(waiting);
		createOrUpdateButton.setDisable(waiting);
		cancelButton.setDisable(waiting);
		progressBar.setVisible(waiting);
	}

	private void checkCreatableOrUpdatable()
	{
		createOrUpdateButton.setDisable((boardId == 0L && boardName.getText().isBlank()) ||
				(boardId == 0L && boardDescription.getText().isBlank()) ||
				(
						Strings.CS.equals(initialName, boardName.getText()) &&
								Strings.CS.equals(initialDescription, boardDescription.getText()) &&
								Strings.CS.equals(initialUrl, boardLogo.getUrl())
				)
		);
	}

	private void selectGroupImage(ActionEvent event)
	{
		var fileChooser = new FileChooser();
		fileChooser.setTitle(bundle.getString("board.select-logo"));
		ChooserUtils.setInitialDirectory(fileChooser, OsUtils.getDownloadDir());
		ChooserUtils.setSupportedLoadImageFormats(fileChooser);
		var selectedFile = fileChooser.showOpenDialog(getWindow(event));
		boardLogo.setFile(selectedFile);
	}

	private void clearGroupImage(ActionEvent event)
	{
		boardLogo.setImage(null);
	}
}
