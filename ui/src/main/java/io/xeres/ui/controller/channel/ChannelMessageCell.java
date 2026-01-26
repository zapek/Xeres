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
import io.xeres.ui.client.ChannelClient;
import io.xeres.ui.client.GeneralClient;
import io.xeres.ui.custom.asyncimage.AsyncImageView;
import io.xeres.ui.model.channel.ChannelMessage;
import io.xeres.ui.support.util.DateUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import org.fxmisc.flowless.Cell;

import java.io.IOException;
import java.util.Map;

import static io.xeres.common.rest.PathConfig.CHANNELS_PATH;

class ChannelMessageCell implements Cell<ChannelMessage, Node>
{
	@FXML
	private HBox groupView;

	@FXML
	private Label titleLabel;

	@FXML
	private Label postInstantLabel;

	@FXML
	private ToggleButton unreadButton;

	@FXML
	private AsyncImageView imageView;

	public ChannelMessageCell(ChannelMessage channelMessage, GeneralClient generalClient, ChannelClient channelClient)
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

		unreadButton.setOnAction(_ -> {
			var item = (ChannelMessage) unreadButton.getUserData();
			item.setRead(!unreadButton.isSelected());
			channelClient.updateChannelMessagesRead(Map.of(item.getId(), item.isRead()))
					.subscribe();
		});
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
	public void reset()
	{
		imageView.setUrl(null);
	}

	@Override
	public void updateItem(ChannelMessage item)
	{
		titleLabel.setText(item.getName());
		postInstantLabel.setText(DateUtils.DATE_TIME_FORMAT.format(item.getPublished()));
		unreadButton.setSelected(!item.isRead());
		unreadButton.setUserData(item);
		setAspectRatio(item);
		imageView.setUrl(getImageUrl(item));
	}

	private String getImageUrl(ChannelMessage item)
	{
		if (item.hasImage())
		{
			return RemoteUtils.getControlUrl() + CHANNELS_PATH + "/messages/" + item.getId() + "/image";
		}
		return null;
	}

	private void setAspectRatio(ChannelMessage item)
	{
		// XXX: play around with the aspect ratio. for now we guess it on the fly but it should be a "per channel guess" then stored somewhere
		if (item.hasImage())
		{
			var ratio = (double) item.getImageWidth() / item.getImageHeight();

			if (ratio > 1.4)
			{
				// 16:9
				imageView.setFitWidth(171.0);
				imageView.setFitHeight(96.0);
			}
			else if (ratio < 0.8)
			{
				// 3:4
				imageView.setFitWidth(111.0);
				imageView.setFitHeight(148.0);
			}
			else
			{
				// 1:1
				imageView.setFitWidth(128.0);
				imageView.setFitHeight(128.0);
			}
		}
		else
		{
			imageView.setFitWidth(128.0);
			imageView.setFitHeight(128.0);
		}
	}
}
