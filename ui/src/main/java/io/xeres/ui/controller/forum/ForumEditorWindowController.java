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

import io.xeres.common.rest.forum.ForumPostRequest;
import io.xeres.ui.client.ForumClient;
import io.xeres.ui.client.LocationClient;
import io.xeres.ui.controller.WindowController;
import io.xeres.ui.custom.EditorView;
import io.xeres.ui.model.forum.ForumMessage;
import io.xeres.ui.support.markdown.MarkdownService;
import io.xeres.ui.support.util.UiUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.stereotype.Component;

import java.util.ResourceBundle;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Component
@FxmlView(value = "/view/forum/forum_editor_view.fxml")
public class ForumEditorWindowController implements WindowController
{
	@FXML
	private TextField forumName;

	@FXML
	private TextField title; // XXX: should be disabled, then enabled

	@FXML
	private EditorView editorView; // XXX: should be disabled, then enabled

	@FXML
	private Button send;

	private ForumPostRequest forumPostRequest;

	private final ForumClient forumClient;
	private final LocationClient locationClient;
	private final MarkdownService markdownService;
	private final ResourceBundle bundle;

	public ForumEditorWindowController(ForumClient forumClient, LocationClient locationClient, MarkdownService markdownService, ResourceBundle bundle)
	{
		this.forumClient = forumClient;
		this.locationClient = locationClient;
		this.markdownService = markdownService;
		this.bundle = bundle;
	}

	@Override
	public void initialize()
	{
		Platform.runLater(() -> title.requestFocus());

		editorView.lengthProperty.addListener((_, _, newValue) -> checkSendable((Integer) newValue));
		editorView.setInputContextMenu(locationClient);
		editorView.setMarkdownService(markdownService);
		title.setOnKeyTyped(_ -> checkSendable(editorView.lengthProperty.getValue()));

		send.setOnAction(_ -> postMessage());
	}

	@Override
	public void onShown()
	{
		var userData = UiUtils.getUserData(title);
		if (userData == null)
		{
			throw new IllegalArgumentException("Missing PostRequest");
		}

		forumPostRequest = (ForumPostRequest) userData;

		forumClient.getForumGroupById(forumPostRequest.forumId())
				.doOnSuccess(forumGroup -> Platform.runLater(() -> {
					assert forumGroup != null;
					forumName.setText(forumGroup.getName());
				}))
				.subscribe();

		if (forumPostRequest.replyToId() != 0L)
		{
			title.setDisable(true);
			forumClient.getForumMessage(forumPostRequest.replyToId())
					.doOnSuccess(forumMessage -> Platform.runLater(() -> {
						assert forumMessage != null;
						addReply(forumMessage);
					}))
					.subscribe();
		}

		// Prevent the message from being discarded by mistake
		UiUtils.getWindow(send).setOnCloseRequest(event -> {
			if (editorView.isModified())
			{
				UiUtils.showAlertConfirm(bundle.getString("forum.editor.cancel"), () -> UiUtils.getWindow(send).hide());
				event.consume();
			}
		});
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
		// XXX: add a spinner delay, then clear it on error
		forumClient.createForumMessage(forumPostRequest.forumId(), title.getText(), editorView.getText(), forumPostRequest.replyToId(), forumPostRequest.originalId())
				.doOnSuccess(_ -> Platform.runLater(() -> UiUtils.closeWindow(forumName)))
				.subscribe();
	}
}
