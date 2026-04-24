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

package io.xeres.ui.controller.channel;

import io.xeres.common.util.RemoteUtils;
import io.xeres.ui.client.GeneralClient;
import io.xeres.ui.custom.asyncimage.PlaceholderImageView;
import io.xeres.ui.model.channel.ChannelMessage;
import io.xeres.ui.support.util.DateUtils;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import org.fxmisc.flowless.Cell;

import java.io.IOException;

import static io.xeres.common.rest.PathConfig.CHANNELS_PATH;

class ChannelMessageCell implements Cell<ChannelMessage, Node>
{
	private static final PseudoClass selectedPseudoClass = PseudoClass.getPseudoClass("selected");
	private static final PseudoClass unreadPseudoClass = PseudoClass.getPseudoClass("unread");

	@FXML
	private HBox groupView;

	@FXML
	private Label titleLabel;

	@FXML
	private Label postInstantLabel;

	@FXML
	private PlaceholderImageView imageView;

	public ChannelMessageCell(ChannelMessage channelMessage, GeneralClient generalClient)
	{
		var loader = new FXMLLoader(ChannelMessageCell.class.getResource("/view/channel/message_cell.fxml"));
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
		updateItem(channelMessage);
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
	public void updateItem(ChannelMessage item)
	{
		titleLabel.setText(item.getName());
		postInstantLabel.setText(DateUtils.DATE_TIME_FORMAT.format(item.getPublished()));
		imageView.setUrl(getImageUrl(item));
		groupView.pseudoClassStateChanged(selectedPseudoClass, item.isSelected());
		groupView.pseudoClassStateChanged(unreadPseudoClass, !item.isRead());
	}

	private String getImageUrl(ChannelMessage item)
	{
		if (item.hasImage())
		{
			return RemoteUtils.getControlUrl() + CHANNELS_PATH + "/messages/" + item.getId() + "/image";
		}
		return null;
	}
}
