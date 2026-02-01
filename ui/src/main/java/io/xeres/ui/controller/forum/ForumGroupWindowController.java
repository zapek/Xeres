/*
 * Copyright (c) 2023-2026 by David Gerber - https://zapek.com
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
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import net.rgielen.fxweaver.core.FxmlView;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Component;

import java.util.ResourceBundle;

@Component
@FxmlView(value = "/view/forum/forum_group_view.fxml")
public class ForumGroupWindowController implements WindowController
{
	@FXML
	private Button createOrUpdateButton;

	@FXML
	private Button cancelButton;

	@FXML
	private TextField forumName;

	@FXML
	private TextField forumDescription;

	@FXML
	private ProgressBar progressBar;

	private final ForumClient forumClient;
	private final ResourceBundle bundle;

	private long forumId;

	private String initialName;
	private String initialDescription;

	public ForumGroupWindowController(ForumClient forumClient, ResourceBundle bundle)
	{
		this.forumClient = forumClient;
		this.bundle = bundle;
	}

	@Override
	public void initialize()
	{
		forumName.textProperty().addListener(_ -> checkCreatable());
		forumDescription.textProperty().addListener(_ -> checkCreatable());

		cancelButton.setOnAction(UiUtils::closeWindow);
	}

	@Override
	public void onShown()
	{
		var userData = UiUtils.getUserData(forumName);
		if (userData != null)
		{
			forumId = (long) userData;
		}

		if (forumId != 0L)
		{
			forumClient.getForumGroupById(forumId)
					.doOnSuccess(forumGroup -> Platform.runLater(() -> {
						assert forumGroup != null;
						forumName.setText(forumGroup.getName());
						forumDescription.setText(forumGroup.getDescription());
						initialName = forumName.getText();
						initialDescription = forumDescription.getText();
						createOrUpdateButton.setDisable(true);
					}))
					.subscribe();
			createOrUpdateButton.setText("Update");
			createOrUpdateButton.setOnAction(_ -> {
				setWaiting(true);
				forumClient.updateForumGroup(forumId,
								forumName.getText(),
								forumDescription.getText())
						.doOnSuccess(_ -> Platform.runLater(() -> UiUtils.closeWindow(forumName)))
						.doFinally(_ -> setWaiting(false))
						.subscribe();
			});
		}
		else
		{
			createOrUpdateButton.setOnAction(_ -> {
				setWaiting(true);
				forumClient.createForumGroup(forumName.getText(),
								forumDescription.getText())
						.doOnSuccess(_ -> Platform.runLater(() -> UiUtils.closeWindow(forumName)))
						.doFinally(_ -> setWaiting(false))
						.subscribe();
			});
		}
	}

	private void setWaiting(boolean waiting)
	{
		forumName.setDisable(waiting);
		forumDescription.setDisable(waiting);
		createOrUpdateButton.setDisable(waiting);
		cancelButton.setDisable(waiting);
		progressBar.setVisible(waiting);
	}

	private void checkCreatable()
	{
		createOrUpdateButton.setDisable(forumId == 0L && forumName.getText().isBlank() ||
				(forumId == 0L && forumDescription.getText().isBlank()) ||
				(
						Strings.CS.equals(initialName, forumName.getText()) &&
								Strings.CS.equals(initialDescription, forumDescription.getText())
				)
		);
	}
}
