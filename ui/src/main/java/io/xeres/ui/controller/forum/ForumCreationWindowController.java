/*
 * Copyright (c) 2023 by David Gerber - https://zapek.com
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

package io.xeres.ui.controller.forum;

import io.xeres.ui.client.ForumClient;
import io.xeres.ui.controller.WindowController;
import io.xeres.ui.support.util.UiUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@FxmlView(value = "/view/forum/forum_create.fxml")
public class ForumCreationWindowController implements WindowController
{
	@FXML
	private Button createButton;

	@FXML
	private Button cancelButton;

	@FXML
	private TextField forumName;

	@FXML
	private TextField forumDescription;

	private final ForumClient forumClient;

	public ForumCreationWindowController(ForumClient forumClient)
	{
		this.forumClient = forumClient;
	}

	@Override
	public void initialize() throws IOException
	{
		forumName.textProperty().addListener(observable -> checkCreatable());
		forumDescription.textProperty().addListener(observable -> checkCreatable());

		createButton.setOnAction(event -> forumClient.createForumGroup(forumName.getText(),
						forumDescription.getText())
				.doOnSuccess(aVoid -> Platform.runLater(() -> UiUtils.closeWindow(forumName)))
				.subscribe());
		cancelButton.setOnAction(UiUtils::closeWindow);
	}

	private void checkCreatable()
	{
		createButton.setDisable(forumName.getText().isBlank() || forumDescription.getText().isBlank());
	}
}
