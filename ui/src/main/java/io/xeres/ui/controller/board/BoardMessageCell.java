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

import io.xeres.common.util.RemoteUtils;
import io.xeres.ui.client.GeneralClient;
import io.xeres.ui.custom.DisclosedHyperlink;
import io.xeres.ui.custom.asyncimage.AsyncImageView;
import io.xeres.ui.model.board.BoardMessage;
import io.xeres.ui.support.util.DateUtils;
import io.xeres.ui.support.util.UiUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.fxmisc.flowless.Cell;

import java.io.IOException;

import static io.xeres.common.rest.PathConfig.BOARDS_PATH;

class BoardMessageCell implements Cell<BoardMessage, Node>
{
	@FXML
	private VBox groupView;

	@FXML
	private Label titleLabel;

	@FXML
	private DisclosedHyperlink linkView;

	@FXML
	private Label authorLabel;

	@FXML
	private Label postInstantLabel;

	@FXML
	private AsyncImageView imageView;

	public BoardMessageCell(BoardMessage boardMessage, GeneralClient generalClient)
	{
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
		// XXX: set image cache? it will have to be bigger I think, too

		// XXX: linkView must have a proper action! both for retroshare:// and external ones...


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
		if (item.hasLink())
		{
			linkView.setText(item.getName());
			linkView.setUri(item.getLink());
			UiUtils.setPresent(linkView);
			UiUtils.setAbsent(titleLabel);
		}
		else
		{
			titleLabel.setText(item.getName());
			UiUtils.setPresent(titleLabel);
			UiUtils.setAbsent(linkView);
		}
		authorLabel.setText(item.getAuthorName());
		postInstantLabel.setText(DateUtils.DATE_TIME_DISPLAY.format(item.getPublished()));
		UiUtils.setPresent(imageView, item.hasImage());
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
