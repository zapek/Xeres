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

package io.xeres.ui.controller.channel;

import io.xeres.common.id.GxsId;
import io.xeres.common.rest.notification.channel.AddOrUpdateChannelGroups;
import io.xeres.common.rest.notification.channel.AddOrUpdateChannelMessages;
import io.xeres.common.rest.notification.channel.MarkAllChannelMessagesAsRead;
import io.xeres.common.rest.notification.channel.MarkChannelMessagesAsRead;
import io.xeres.common.util.RemoteUtils;
import io.xeres.ui.client.ChannelClient;
import io.xeres.ui.client.GeneralClient;
import io.xeres.ui.client.NotificationClient;
import io.xeres.ui.controller.Controller;
import io.xeres.ui.controller.common.GxsGroupTreeTableAction;
import io.xeres.ui.controller.common.GxsGroupTreeTableView;
import io.xeres.ui.custom.asyncimage.ImageCache;
import io.xeres.ui.event.UnreadEvent;
import io.xeres.ui.model.channel.ChannelGroup;
import io.xeres.ui.model.channel.ChannelMapper;
import io.xeres.ui.model.channel.ChannelMessage;
import io.xeres.ui.support.clipboard.ClipboardUtils;
import io.xeres.ui.support.loader.OnDemandLoader;
import io.xeres.ui.support.unread.UnreadService;
import io.xeres.ui.support.uri.ChannelUri;
import io.xeres.ui.support.util.UiUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import net.rgielen.fxweaver.core.FxmlView;
import org.fxmisc.flowless.VirtualFlow;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import tools.jackson.databind.json.JsonMapper;

import java.util.*;

import static io.xeres.common.rest.PathConfig.CHANNELS_PATH;
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

	@FXML
	private Button createChannel;

	@FXML
	private Button newPost;

	@FXML
	private StackPane contentGroup;

	private final ObservableList<ChannelMessage> messages = FXCollections.observableArrayList();

	private OnDemandLoader<ChannelGroup, ChannelMessage> onDemandLoader;

	private final ResourceBundle bundle;

	private final ChannelClient channelClient;
	private final NotificationClient notificationClient;
	private final GeneralClient generalClient;
	private final ImageCache imageCache;
	private final UnreadService unreadService;
	private final JsonMapper jsonMapper;

	private Disposable notificationDisposable;

	public ChannelViewController(ResourceBundle bundle, ChannelClient channelClient, NotificationClient notificationClient, GeneralClient generalClient, ImageCache imageCache, UnreadService unreadService, JsonMapper jsonMapper)
	{
		this.channelClient = channelClient;
		this.bundle = bundle;

		this.notificationClient = notificationClient;
		this.generalClient = generalClient;
		this.imageCache = imageCache;
		this.unreadService = unreadService;
		this.jsonMapper = jsonMapper;
	}

	@Override
	public void initialize()
	{
		log.debug("Trying to get channel list...");
		channelTree.initialize(CHANNELS,
				channelClient,
				ChannelGroup::new,
				() -> new ChannelGroupCell(generalClient, imageCache),
				this,
				hasUnreadMessages -> unreadService.sendUnreadEvent(UnreadEvent.Element.CHANNEL, hasUnreadMessages));

		// VirtualizedScrollPane doesn't work from FXML so we add it manually
		VirtualizedScrollPane<VirtualFlow<ChannelMessage, ChannelMessageCell>> messagesView = new VirtualizedScrollPane<>(VirtualFlow.createVertical(messages, channelMessage -> new ChannelMessageCell(channelMessage, generalClient, channelClient)));
		VBox.setVgrow(messagesView, Priority.ALWAYS);
		contentGroup.getChildren().add(messagesView);

		onDemandLoader = new OnDemandLoader<>(messagesView, messages, channelClient);

		// XXX: actions for create channel and create post

		setupChannelNotifications();
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
		var channelUri = new ChannelUri(group.getName(), group.getGxsId(), null);
		ClipboardUtils.copyTextToClipboard(channelUri.toUriString());
	}

	@Override
	public void onSelectSubscribed(ChannelGroup group)
	{
		onDemandLoader.changeSelection(group);
		newPost.setDisable(false);
	}

	@Override
	public void onSelectUnsubscribed(ChannelGroup group)
	{
		onDemandLoader.changeSelection(group);
		newPost.setDisable(true);
	}

	@Override
	public void onUnselect()
	{
		onDemandLoader.changeSelection(null);
		newPost.setDisable(true);
	}

	@Override
	public void onEdit(ChannelGroup group)
	{
		// XXX: open channel creation
	}

	@Override
	public void onMarkAllAsRead(ChannelGroup group, boolean read)
	{
		messages.forEach(channelMessage -> channelMessage.setRead(read)); // XXX: this won't refresh what is visible, only what gets scrolled
	}

	@EventListener
	public void onApplicationEvent(ContextClosedEvent ignored)
	{
		if (notificationDisposable != null && !notificationDisposable.isDisposed())
		{
			notificationDisposable.dispose();
		}
	}

	private void setupChannelNotifications()
	{
		notificationDisposable = notificationClient.getChannelNotifications()
				.doOnError(UiUtils::webAlertError)
				.doOnNext(sse -> Platform.runLater(() -> {
					if (sse.data() != null)
					{
						var idName = Objects.requireNonNull(sse.id());

						if (idName.equals(AddOrUpdateChannelGroups.class.getSimpleName()))
						{
							var action = jsonMapper.convertValue(sse.data().action(), AddOrUpdateChannelGroups.class);

							action.channelGroups().forEach(channelGroupItem -> imageCache.evictImage(RemoteUtils.getControlUrl() + CHANNELS_PATH + "/groups/" + channelGroupItem.id() + "/image"));

							channelTree.addGroups(action.channelGroups().stream()
									.map(ChannelMapper::fromDTO)
									.toList());
						}
						else if (idName.equals(AddOrUpdateChannelMessages.class.getSimpleName()))
						{
							var action = jsonMapper.convertValue(sse.data().action(), AddOrUpdateChannelMessages.class);

							addChannelMessages(action.channelMessages().stream()
									.map(ChannelMapper::fromDTO)
									.toList());
						}
						else if (idName.equals(MarkChannelMessagesAsRead.class.getSimpleName()))
						{
							var action = jsonMapper.convertValue(sse.data().action(), MarkChannelMessagesAsRead.class);

							markChannelMessagesAsRead(action.messageMap());
						}
						else if (idName.equals(MarkAllChannelMessagesAsRead.class.getSimpleName()))
						{
							var action = jsonMapper.convertValue(sse.data().action(), MarkAllChannelMessagesAsRead.class);

							markAllChannelMessagesAsRead(action.groupId(), action.updateCount());
						}
						else
						{
							log.debug("Unknown channel notification");
						}
					}
				}))
				.subscribe();
	}

	private void markChannelMessagesAsRead(Map<Long, Boolean> messageMap)
	{
		// Handle the most common case quickly
		if (messageMap.size() == 1)
		{
			var message = messageMap.entrySet().iterator().next();
			channelTree.getSelectedGroup().addUnreadCount(message.getValue() ? -1 : 1);
			channelTree.refreshTree();
			return;
		}

		messageMap.forEach((_, _) -> {
			// XXX: implement... boring. not needed yet because we can't mark several entries at once
		});
	}

	private void markAllChannelMessagesAsRead(long groupId, int updateCount)
	{
		channelTree.getSubscribedGroups()
				.filter(channelGroupTreeItem -> channelGroupTreeItem.getValue().getId() == groupId)
				.findFirst().ifPresent(channelGroupTreeItem -> {
					channelGroupTreeItem.getValue().addUnreadCount(updateCount);
					channelTree.refreshTree();
				});
	}

	private void addChannelMessages(List<ChannelMessage> channelMessages)
	{
		Map<GxsId, Integer> channelsToSetCount = new HashMap<>();
		var selectedChannelGroup = channelTree.getSelectedGroup();

		for (ChannelMessage channelMessage : channelMessages)
		{
			if (selectedChannelGroup != null && channelMessage.getGxsId().equals(selectedChannelGroup.getGxsId()))
			{
				onDemandLoader.insertMessage(channelMessage);
			}
			channelsToSetCount.merge(channelMessage.getGxsId(), 1, Integer::sum);
		}
		channelTree.addUnreadCount(channelsToSetCount);
	}
}
