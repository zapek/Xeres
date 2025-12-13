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

package io.xeres.ui.controller.channel;

import io.xeres.ui.client.ChannelClient;
import io.xeres.ui.client.GeneralClient;
import io.xeres.ui.client.NotificationClient;
import io.xeres.ui.controller.Controller;
import io.xeres.ui.controller.common.GxsGroupTreeTableAction;
import io.xeres.ui.controller.common.GxsGroupTreeTableView;
import io.xeres.ui.custom.asyncimage.ImageCache;
import io.xeres.ui.model.channel.ChannelGroup;
import javafx.fxml.FXML;
import javafx.scene.control.SplitPane;
import net.rgielen.fxweaver.core.FxmlView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ResourceBundle;

import static io.xeres.ui.support.preference.PreferenceUtils.CHANNELS;

@Component
@FxmlView(value = "/view/channel/channel_view.fxml")
public class ChannelViewController implements Controller, GxsGroupTreeTableAction<ChannelGroup>
{
	private static final Logger log = LoggerFactory.getLogger(ChannelViewController.class);

	@FXML
	private GxsGroupTreeTableView<ChannelGroup> channelTree;

	@FXML
	private SplitPane splitPaneVertical;

	private final ResourceBundle bundle;

	private final ChannelClient channelClient;
	private final NotificationClient notificationClient;
	private final GeneralClient generalClient;
	private final ImageCache imageCache;

	public ChannelViewController(ResourceBundle bundle, ChannelClient channelClient, NotificationClient notificationClient, GeneralClient generalClient, ImageCache imageCache)
	{
		this.channelClient = channelClient;
		this.bundle = bundle;

		this.notificationClient = notificationClient;
		this.generalClient = generalClient;
		this.imageCache = imageCache;
	}

	@Override
	public void initialize()
	{
		log.debug("Trying to get channel list...");
		channelTree.initialize(CHANNELS, ChannelGroup::new, () -> new ChannelCell(generalClient, imageCache), this);
	}

	@Override
	public void onSubscribe(ChannelGroup group)
	{

	}

	@Override
	public void onUnsubscribe(ChannelGroup group)
	{

	}

	@Override
	public void onCopyLink(ChannelGroup group)
	{

	}

	@Override
	public void onSelect(ChannelGroup group)
	{

	}
}
