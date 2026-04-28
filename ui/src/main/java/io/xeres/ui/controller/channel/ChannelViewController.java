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
import io.xeres.common.id.MsgId;
import io.xeres.common.id.Sha1Sum;
import io.xeres.common.rest.notification.channel.AddOrUpdateChannelGroups;
import io.xeres.common.rest.notification.channel.AddOrUpdateChannelMessages;
import io.xeres.common.rest.notification.channel.SetChannelGroupMessagesReadState;
import io.xeres.common.rest.notification.channel.SetChannelMessageReadState;
import io.xeres.common.util.RemoteUtils;
import io.xeres.ui.client.ChannelClient;
import io.xeres.ui.client.GeneralClient;
import io.xeres.ui.client.NotificationClient;
import io.xeres.ui.controller.Controller;
import io.xeres.ui.controller.common.GxsGroupTreeTableAction;
import io.xeres.ui.controller.common.GxsGroupTreeTableView;
import io.xeres.ui.custom.ProgressPane;
import io.xeres.ui.custom.asyncimage.AsyncImageView;
import io.xeres.ui.custom.asyncimage.ImageCache;
import io.xeres.ui.event.OpenUriEvent;
import io.xeres.ui.event.UnreadEvent;
import io.xeres.ui.model.channel.ChannelFile;
import io.xeres.ui.model.channel.ChannelGroup;
import io.xeres.ui.model.channel.ChannelMapper;
import io.xeres.ui.model.channel.ChannelMessage;
import io.xeres.ui.support.clipboard.ClipboardUtils;
import io.xeres.ui.support.contentline.Content;
import io.xeres.ui.support.loader.OnDemandLoader;
import io.xeres.ui.support.loader.OnDemandLoaderAction;
import io.xeres.ui.support.markdown.MarkdownService;
import io.xeres.ui.support.unread.UnreadService;
import io.xeres.ui.support.uri.ChannelUri;
import io.xeres.ui.support.uri.FileUriFactory;
import io.xeres.ui.support.util.DateUtils;
import io.xeres.ui.support.util.TextFlowDragSelection;
import io.xeres.ui.support.util.UiUtils;
import io.xeres.ui.support.window.WindowManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;
import net.rgielen.fxweaver.core.FxmlView;
import org.apache.commons.lang3.StringUtils;
import org.fxmisc.flowless.VirtualFlow;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;

import java.util.*;
import java.util.stream.Collectors;

import static io.xeres.common.rest.PathConfig.CHANNELS_PATH;
import static io.xeres.ui.support.preference.PreferenceUtils.CHANNELS;
import static io.xeres.ui.support.util.DateUtils.DATE_TIME_PRECISE_FORMAT;
import static javafx.scene.control.Alert.AlertType.WARNING;

@Component
@FxmlView(value = "/view/channel/channel_view.fxml")
public class ChannelViewController implements Controller, GxsGroupTreeTableAction<ChannelGroup>, OnDemandLoaderAction<ChannelGroup>
{
	@FXML
	private GxsGroupTreeTableView<ChannelGroup> channelTree;

	@FXML
	private SplitPane splitPaneVertical;

	@FXML
	private SplitPane splitPaneHorizontal;

	@FXML
	private Button createChannel;

	@FXML
	private Button newPost;

	@FXML
	private ProgressPane channelMessagesProgress;

	@FXML
	private ScrollPane messagePane;

	@FXML
	private AsyncImageView imageHeader;

	@FXML
	private TextFlow messageHeader;

	@FXML
	private TextFlow messageContent;

	private final ObservableList<ChannelMessage> messages = FXCollections.observableArrayList();

	private OnDemandLoader<ChannelGroup, ChannelMessage> onDemandLoader;

	private final ResourceBundle bundle;

	private final ChannelClient channelClient;
	private final NotificationClient notificationClient;
	private final GeneralClient generalClient;
	private final ImageCache imageCache;
	private final UnreadService unreadService;
	private final WindowManager windowManager;
	private final MarkdownService markdownService;

	private Disposable notificationDisposable;

	private ChannelMessage selectedChannelMessage;

	private UrlToOpen urlToOpen;

	public ChannelViewController(ResourceBundle bundle, ChannelClient channelClient, NotificationClient notificationClient, GeneralClient generalClient, ImageCache imageCache, UnreadService unreadService, WindowManager windowManager, MarkdownService markdownService)
	{
		this.channelClient = channelClient;
		this.bundle = bundle;

		this.notificationClient = notificationClient;
		this.generalClient = generalClient;
		this.imageCache = imageCache;
		this.unreadService = unreadService;
		this.windowManager = windowManager;
		this.markdownService = markdownService;
	}

	@Override
	public void initialize()
	{
		channelTree.initialize(CHANNELS,
				channelClient,
				ChannelGroup::new,
				() -> new ChannelGroupCell(generalClient, imageCache),
				this);

		channelTree.unreadProperty().addListener((_, _, newValue) -> unreadService.sendUnreadEvent(UnreadEvent.Element.CHANNEL, newValue));

		// VirtualizedScrollPane doesn't work from FXML so we add it manually
		VirtualizedScrollPane<VirtualFlow<ChannelMessage, ChannelMessageCell>> messagesView = new VirtualizedScrollPane<>(VirtualFlow.createVertical(messages, channelMessage -> new ChannelMessageCell(channelMessage, generalClient)));
		VBox.setVgrow(messagesView, Priority.ALWAYS);
		channelMessagesProgress.getChildren().add(messagesView);

		onDemandLoader = new OnDemandLoader<>(messagesView, messages, channelClient, this);

		createChannel.setOnAction(_ -> windowManager.openChannelCreation(0L));

		newPost.setOnAction(_ -> newChannelPost());

		TextFlowDragSelection.enableSelection(messageHeader, messagePane);
		TextFlowDragSelection.enableSelection(messageContent, messagePane);

		imageHeader.setLoader(url -> generalClient.getImage(url).block());

		messagesView.setOnMouseClicked(event -> {
			var hit = messagesView.getContent().hit(event.getX(), event.getY());
			if (hit.isCellHit())
			{
				changeSelectedChannelMessage(hit.getCellIndex());
			}
		});

		setupChannelNotifications();
	}

	@EventListener
	public void handleOpenUriEvent(OpenUriEvent event)
	{
		if (event.uri() instanceof ChannelUri channelUri)
		{
			if (!channelTree.openUrl(channelUri.gxsId(), channelUri.msgId()))
			{
				UiUtils.showAlert(WARNING, bundle.getString("channel.view.group.not-found"));
			}
		}
	}

	private void changeSelectedChannelMessage(int index)
	{
		if (index >= 0)
		{
			var channelMessage = messages.get(index);
			if (Objects.equals(selectedChannelMessage, channelMessage))
			{
				return;
			}

			clearSelected();
			selectedChannelMessage = channelMessage;
			channelMessage.setSelected(true);
			messages.set(index, channelMessage);

			channelClient.getChannelMessage(channelMessage.getId())
					.doOnSuccess(message -> Platform.runLater(() -> {
						assert message != null;
						setCommonMessageAttributes(message);
						// XXX: multiple versions?
						if (!message.isRead())
						{
							channelClient.setChannelMessageReadState(message.getId(), true)
									.subscribe();
						}
					}))
					.doOnError(UiUtils::webAlertError)
					.subscribe();
		}
	}

	private void setCommonMessageAttributes(ChannelMessage message)
	{
		messageHeader.getChildren().clear();
		messageContent.getChildren().clear();
		messagePane.setVvalue(messagePane.getVmin());
		if (message.hasImage())
		{
			imageHeader.setFitWidth(message.getImageWidth());
			imageHeader.setFitHeight(message.getImageHeight());
			imageHeader.setUrl(RemoteUtils.getControlUrl() + CHANNELS_PATH + "/messages/" + message.getId() + "/image");
		}
		else
		{
			imageHeader.setUrl(null);
			imageHeader.setFitWidth(0);
			imageHeader.setFitHeight(0);
		}

		addHeaderContent("## " + message.getName() +
				"\n\n#### " + DATE_TIME_PRECISE_FORMAT.format(message.getPublished()));
		addMessageContent(StringUtils.defaultString(message.getContent()) + "\n\n" +
				getFiles(message.getFiles()));
	}

	private void setCommonGroupAttributes(ChannelGroup group)
	{
		messageHeader.getChildren().clear();
		messageContent.getChildren().clear();
		messagePane.setVvalue(messagePane.getVmin());
		if (group != null && group.hasImage())
		{
			imageHeader.setFitWidth(128);
			imageHeader.setFitHeight(128);
			imageHeader.setUrl(RemoteUtils.getControlUrl() + CHANNELS_PATH + "/groups/" + group.getId() + "/image");
		}
		else
		{
			imageHeader.setUrl(null);
			imageHeader.setFitWidth(0);
			imageHeader.setFitHeight(0);
		}

		if (group != null && group.isReal())
		{
			addHeaderContent("""
					## %s
					
					%s: %s\\
					%s: %s
					""".formatted(
					group.getName(),
					bundle.getString("posts-at-remote-nodes"),
					group.getVisibleMessageCount(),
					bundle.getString("last-activity"),
					DateUtils.formatDateTime(group.getLastActivity(), bundle.getString("unknown-lc"))
			));
		}
	}

	private String getFiles(List<ChannelFile> files)
	{
		var result = files.isEmpty() ? "" : "\n\n### %s\n\n- ".formatted(bundle.getString("channel.files"));
		result += files.stream()
				.map(file -> FileUriFactory.generateMarkdown(file.getName(), file.getSize(), Sha1Sum.fromString(file.getHash())))
				.collect(Collectors.joining("\n- "));
		return result;
	}

	private void clearSelected()
	{
		if (selectedChannelMessage != null)
		{
			selectedChannelMessage.setSelected(false);
			messages.set(messages.indexOf(selectedChannelMessage), selectedChannelMessage);
		}
	}

	@Override
	public void onSubscribeToGroup(ChannelGroup group)
	{

	}

	@Override
	public void onUnsubscribeFromGroup(ChannelGroup group)
	{

	}

	@Override
	public void onCopyGroupLink(ChannelGroup group)
	{
		var channelUri = new ChannelUri(group.getName(), group.getGxsId(), null);
		ClipboardUtils.copyTextToClipboard(channelUri.toUriString());
	}

	@Override
	public void onOpenUrl(GxsId gxsId, MsgId msgId)
	{
		if (gxsId.equals(channelTree.getSelectedGroupGxsId()))
		{
			selectMessage(msgId);
		}
		urlToOpen = new UrlToOpen(gxsId, msgId);
	}

	@Override
	public void onMessagesLoaded(ChannelGroup group)
	{
		channelMessagesState(false);
		if (urlToOpen != null)
		{
			if (group.getGxsId().equals(urlToOpen.gxsId()))
			{
				selectMessage(urlToOpen.msgId());
				urlToOpen = null;
			}
		}
	}

	private void selectMessage(MsgId msgId)
	{
		for (var i = 0; i < messages.size(); i++)
		{
			var message = messages.get(i);
			if (message.getMsgId().equals(msgId))
			{
				changeSelectedChannelMessage(i);
				break;
			}
		}
	}

	@Override
	public void onSelectSubscribedGroup(ChannelGroup group)
	{
		selectedChannelMessage = null;
		channelMessagesState(true);
		onDemandLoader.changeSelection(group);
		newPost.setDisable(group.isExternal());
		showInfo(group);
	}

	@Override
	public void onSelectUnsubscribedGroup(ChannelGroup group)
	{
		selectedChannelMessage = null;
		onDemandLoader.changeSelection(group);
		newPost.setDisable(true);
		showInfo(group);
	}

	@Override
	public void onUnselectGroup()
	{
		selectedChannelMessage = null;
		onDemandLoader.changeSelection(null);
		newPost.setDisable(true);
		showInfo(null);
	}

	@Override
	public void onEditGroup(ChannelGroup group)
	{
		windowManager.openChannelCreation(group.getId());
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
					switch (sse.data())
					{
						case AddOrUpdateChannelGroups action ->
						{
							action.channelGroups().forEach(channelGroupItem -> imageCache.evictImage(RemoteUtils.getControlUrl() + CHANNELS_PATH + "/groups/" + channelGroupItem.id() + "/image"));

							channelTree.addGroups(action.channelGroups().stream()
									.map(ChannelMapper::fromDTO)
									.toList());
						}
						case AddOrUpdateChannelMessages action -> addChannelMessages(action.channelMessages().stream()
								.map(ChannelMapper::fromDTO)
								.toList());
						case SetChannelMessageReadState action -> setMessageReadState(action.groupId(), action.messageId(), action.read());
						case SetChannelGroupMessagesReadState action -> setGroupMessagesReadState(action.groupId(), action.read());
						case null -> throw new IllegalArgumentException("Channel notifications have not been set");
					}
				}))
				.subscribe();
	}

	private void setMessageReadState(long groupId, long messageId, boolean read)
	{
		onDemandLoader.setMessageReadState(groupId, messageId, read);
		channelTree.setUnreadCount(groupId, read);
	}

	private void setGroupMessagesReadState(long groupId, boolean read)
	{
		onDemandLoader.setGroupMessagesReadState(groupId, read);
		channelTree.refreshUnreadCount(groupId);
	}

	private void newChannelPost()
	{
		windowManager.openChannelMessage(channelTree.getSelectedGroupId());
	}

	private void addChannelMessages(List<ChannelMessage> channelMessages)
	{
		Set<GxsId> channelsToUpdate = new HashSet<>();

		for (ChannelMessage channelMessage : channelMessages)
		{
			onDemandLoader.insertMessage(channelMessage);
			channelsToUpdate.add(channelMessage.getGxsId());
		}
		channelTree.refreshUnreadCount(channelsToUpdate);
	}

	private void addHeaderContent(String input)
	{
		messageHeader.getChildren().addAll(markdownService.parse(input, EnumSet.noneOf(MarkdownService.Rendering.class)).stream()
				.map(Content::getNode).toList());
	}

	private void addMessageContent(String input)
	{
		messageContent.getChildren().addAll(markdownService.parse(input, EnumSet.noneOf(MarkdownService.Rendering.class)).stream()
				.map(Content::getNode).toList());
	}

	private void channelMessagesState(boolean loading)
	{
		Platform.runLater(() -> channelMessagesProgress.showProgress(loading));
	}

	private void showInfo(ChannelGroup group)
	{
		messageContent.getChildren().clear();
		setCommonGroupAttributes(group);
		if (group != null && group.isReal())
		{
			addMessageContent(group.getDescription());
		}
		channelMessagesState(false);
	}

	record UrlToOpen(GxsId gxsId, MsgId msgId)
	{

	}
}
