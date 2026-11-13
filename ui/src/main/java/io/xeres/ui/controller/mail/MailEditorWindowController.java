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

package io.xeres.ui.controller.mail;

import io.xeres.common.id.GxsId;
import io.xeres.common.id.Identifier;
import io.xeres.common.id.LocationIdentifier;
import io.xeres.ui.client.LocationClient;
import io.xeres.ui.client.MailClient;
import io.xeres.ui.controller.WindowController;
import io.xeres.ui.custom.EditorView;
import io.xeres.ui.support.markdown.MarkdownService;
import io.xeres.ui.support.util.Requester;
import io.xeres.ui.support.util.UiUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;

import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class MailEditorWindowController implements WindowController
{
	@FXML
	private TextField subject;

	@FXML
	private EditorView editorView;

	@FXML
	private ProgressBar progressBar;

	@FXML
	private Button send;

	private CreateMailRequest createMailRequest;

	private final MarkdownService markdownService;
	private final LocationClient locationClient;
	private final MailClient mailClient;
	private final ResourceBundle bundle;

	public MailEditorWindowController(MarkdownService markdownService, LocationClient locationClient, MailClient mailClient, ResourceBundle bundle)
	{
		this.markdownService = markdownService;
		this.locationClient = locationClient;
		this.mailClient = mailClient;
		this.bundle = bundle;
	}

	@Override
	public void initialize()
	{
		// XXX: focus

		editorView.lengthProperty.addListener((_, _, newValue) -> checkSendable((Integer) newValue));
		editorView.setInputContextMenu(locationClient);
		editorView.setMarkdownService(markdownService);
		subject.setOnKeyTyped(_ -> checkSendable(editorView.lengthProperty.getValue()));

		send.setOnAction(_ -> sendMail(Set.of(), Set.of(), Set.of(), subject.getText(), editorView.getText())); // XXX: add real recipients
	}

	@Override
	public void onShown()
	{
		var userData = UiUtils.getUserData(subject);
		if (userData == null)
		{
			throw new IllegalArgumentException("Missing CreateMailRequest");
		}

		createMailRequest = (CreateMailRequest) userData;

		// XXX

		// Prevent the message from being discarded by mistake
		UiUtils.getWindow(send).setOnCloseRequest(event -> {
			if (editorView.isModified())
			{
				Requester.confirm(bundle.getString("mail.editor.cancel"), () -> UiUtils.getWindow(send).hide());
				event.consume();
			}
		});
	}

	private void checkSendable(int editorLength)
	{
		send.setDisable(isBlank(subject.getText()) || editorLength == 0);
	}

	private void setWaiting(boolean waiting)
	{
		subject.setDisable(waiting);
		editorView.setDisable(waiting);
		send.setDisable(waiting);
		UiUtils.setPresent(progressBar, waiting);
	}

	private void sendMail(Set<Identifier> tos, Set<Identifier> ccs, Set<Identifier> bccs, String subject, String content)
	{
		setWaiting(true);

		mailClient.createMailMessage(toLocations(tos), toLocations(ccs), toLocations(bccs), toIdentities(tos), toIdentities(ccs), toIdentities(bccs), subject, content, null)
				.doOnSuccess(_ -> Platform.runLater(() -> UiUtils.closeWindow(editorView)))
				.doOnError(UiUtils::webAlertError)
				.doFinally(_ -> setWaiting(false))
				.subscribe();
	}

	private static Set<LocationIdentifier> toLocations(Set<Identifier> in)
	{
		return in.stream()
				.filter(LocationIdentifier.class::isInstance)
				.map(LocationIdentifier.class::cast)
				.collect(Collectors.toSet());
	}

	private static Set<GxsId> toIdentities(Set<Identifier> in)
	{
		return in.stream()
				.filter(GxsId.class::isInstance)
				.map(GxsId.class::cast)
				.collect(Collectors.toSet());
	}
}
