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

import io.xeres.common.message.forum.ForumMessage;
import io.xeres.common.rest.forum.PostRequest;
import io.xeres.ui.client.ForumClient;
import io.xeres.ui.controller.WindowController;
import io.xeres.ui.custom.EditorView;
import io.xeres.ui.support.util.UiUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Component
@FxmlView(value = "/view/forum/forumeditorview.fxml")
public class ForumEditorViewController implements WindowController
{
	@FXML
	private TextField forumName;

	@FXML
	private TextField title; // XXX: should be disabled, then enabled

	@FXML
	private EditorView editorView; // XXX: should be disabled, then enabled

	@FXML
	private Button send;

	private PostRequest postRequest;

	private final ForumClient forumClient;

	public ForumEditorViewController(ForumClient forumClient)
	{
		this.forumClient = forumClient;
	}

	@Override
	public void initialize() throws IOException
	{
		Platform.runLater(() -> title.requestFocus());

		editorView.lengthProperty.addListener((observable, oldValue, newValue) -> checkSendable((Integer) newValue));
		title.setOnKeyTyped(event -> checkSendable(editorView.lengthProperty.getValue()));

		send.setOnAction(event -> postMessage());
	}

	@Override
	public void onShown()
	{
		var userData = UiUtils.getUserData(title);
		if (userData == null)
		{
			throw new IllegalArgumentException("Missing PostRequest");
		}

		postRequest = (PostRequest) userData;

		forumClient.getForumGroupById(postRequest.forumId())
				.doOnSuccess(forumGroup -> Platform.runLater(() -> forumName.setText(forumGroup.getName())))
				.subscribe();

		if (postRequest.replyToId() != 0L)
		{
			title.setDisable(true);
			forumClient.getForumMessage(postRequest.replyToId())
					.doOnSuccess(forumMessage -> Platform.runLater(() -> addReply(forumMessage)))
					.subscribe();
		}
	}

	private void checkSendable(int editorLength)
	{
		send.setDisable(isBlank(title.getText()) || editorLength == 0);
	}

	private void addReply(ForumMessage forumMessage)
	{
		title.setText((forumMessage.getParentId() == 0L ? "Re: " : "") + forumMessage.getName());
		editorView.setReply(forumMessage.getContent());
	}

	private void postMessage()
	{
		forumClient.createForumMessage(postRequest.forumId(), title.getText(), editorView.getText(), postRequest.replyToId(), postRequest.originalId())
				.doOnSuccess(aVoid -> Platform.runLater(() -> UiUtils.closeWindow(forumName)))
				.subscribe();
	}
}
