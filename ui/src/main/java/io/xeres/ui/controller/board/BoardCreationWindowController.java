/*
 * Copyright (c) 2025 by David Gerber - https://zapek.com
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

import io.xeres.ui.client.BoardClient;
import io.xeres.ui.controller.WindowController;
import io.xeres.ui.custom.ImageSelectorView;
import io.xeres.ui.support.util.UiUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.stereotype.Component;

@Component
@FxmlView(value = "/view/board/board_create.fxml")
public class BoardCreationWindowController implements WindowController
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

	public BoardCreationWindowController(BoardClient boardClient)
	{
		this.boardClient = boardClient;
	}

	@Override
	public void initialize()
	{
		boardName.textProperty().addListener(_ -> checkCreatable());
		boardDescription.textProperty().addListener(_ -> checkCreatable());

		// XXX: add image support. we need a way to display a default image/placeholder when needed...
		// XXX: try to find out if it's not possible to do it with AsyncImageView because it might be desirable when scrolling groups that have no logo... but how?
		//boardLogo.setImage(new Image(Objects.requireNonNull(BoardCreationWindowController.class.getResourceAsStream("/image/egg.png"))));

		createButton.setOnAction(_ -> boardClient.createBoardGroup(boardName.getText(),
						boardDescription.getText())
				.doOnSuccess(_ -> Platform.runLater(() -> UiUtils.closeWindow(boardName)))
				.subscribe());
		cancelButton.setOnAction(UiUtils::closeWindow);
	}

	private void checkCreatable()
	{
		createButton.setDisable(boardName.getText().isBlank() || boardDescription.getText().isBlank());
	}
}
