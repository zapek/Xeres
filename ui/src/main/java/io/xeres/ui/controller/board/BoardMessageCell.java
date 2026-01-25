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

import io.xeres.common.util.RemoteUtils;
import io.xeres.ui.client.BoardClient;
import io.xeres.ui.client.GeneralClient;
import io.xeres.ui.custom.asyncimage.AsyncImageView;
import io.xeres.ui.model.board.BoardMessage;
import io.xeres.ui.support.contentline.Content;
import io.xeres.ui.support.markdown.MarkdownService;
import io.xeres.ui.support.markdown.MarkdownService.ParsingMode;
import io.xeres.ui.support.uri.UriFactory;
import io.xeres.ui.support.util.DateUtils;
import io.xeres.ui.support.util.TextFlowDragSelection;
import io.xeres.ui.support.util.UiUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;
import org.fxmisc.flowless.Cell;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import static io.xeres.common.rest.PathConfig.BOARDS_PATH;

class BoardMessageCell implements Cell<BoardMessage, Node>
{
	@FXML
	private VBox groupView;

	@FXML
	private TextFlow titleFlow;

	@FXML
	private TextFlow contentFlow;

	@FXML
	private Label authorLabel;

	@FXML
	private Label postInstantLabel;

	@FXML
	private ToggleButton unreadButton;

	@FXML
	private AsyncImageView imageView;

	private final MarkdownService markdownService;

	public BoardMessageCell(BoardMessage boardMessage, GeneralClient generalClient, BoardClient boardClient, MarkdownService markdownService)
	{
		this.markdownService = markdownService;

		var loader = new FXMLLoader(BoardMessageCell.class.getResource("/view/board/message_cell.fxml"));
		loader.setController(this);

		try
		{
			loader.load();
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}

		imageView.setLoader(url -> generalClient.getImage(url).block());
		TextFlowDragSelection.enableSelection(contentFlow, null);

		unreadButton.setOnAction(_ -> {
			var item = (BoardMessage) unreadButton.getUserData();
			item.setRead(!unreadButton.isSelected());
			boardClient.updateBoardMessagesRead(Map.of(item.getId(), item.isRead()))
					.subscribe();
		});

		updateItem(boardMessage);
	}

	@Override
	public Node getNode()
	{
		return groupView;
	}

	@Override
	public boolean isReusable()
	{
		return true;
	}

	@Override
	public void reset()
	{
		imageView.setUrl(null);
	}

	@Override
	public void updateItem(BoardMessage item)
	{
		titleFlow.getChildren().clear();
		if (item.hasLink())
		{
			var content = UriFactory.createContent(item.getLink(), item.getName(), markdownService.getUriService());
			titleFlow.getChildren().addAll(content.getNode());
		}
		else
		{
			titleFlow.getChildren().add(new Label(item.getName()));
		}

		contentFlow.getChildren().clear();
		if (item.hasContent())
		{
			contentFlow.getChildren().addAll(markdownService.parse(item.getContent(), Set.of(ParsingMode.PARAGRAPH)).stream()
					.map(Content::getNode).toList());
			UiUtils.setPresent(contentFlow);
		}
		else
		{
			UiUtils.setAbsent(contentFlow);
		}

		authorLabel.setText(item.getAuthorName());
		postInstantLabel.setText(DateUtils.DATE_TIME_FORMAT.format(item.getPublished()));
		unreadButton.setSelected(!item.isRead());
		unreadButton.setUserData(item);
		UiUtils.setPresent(imageView, item.hasImage());
		if (item.hasImage() && item.getImageWidth() > 0 && item.getImageHeight() > 0)
		{
			// Improve layout by knowing the dimension in advance.
			imageView.setFitWidth(item.getImageWidth());
			imageView.setFitHeight(item.getImageHeight());
		}
		imageView.setUrl(getImageUrl(item));
	}

	private String getImageUrl(BoardMessage item)
	{
		if (item.hasImage())
		{
			return RemoteUtils.getControlUrl() + BOARDS_PATH + "/messages/" + item.getId() + "/image";
		}
		return null;
	}
}
