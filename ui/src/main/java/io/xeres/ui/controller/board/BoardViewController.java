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

import io.xeres.common.gxs.GxsGroupConstants;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.MsgId;
import io.xeres.common.rest.notification.board.AddOrUpdateBoardGroups;
import io.xeres.common.rest.notification.board.AddOrUpdateBoardMessages;
import io.xeres.common.rest.notification.board.SetBoardGroupMessagesReadState;
import io.xeres.common.rest.notification.board.SetBoardMessageReadState;
import io.xeres.common.util.RemoteUtils;
import io.xeres.ui.client.BoardClient;
import io.xeres.ui.client.GeneralClient;
import io.xeres.ui.client.NotificationClient;
import io.xeres.ui.controller.Controller;
import io.xeres.ui.controller.common.GxsGroupTreeTableAction;
import io.xeres.ui.controller.common.GxsGroupTreeTableView;
import io.xeres.ui.custom.InfoView;
import io.xeres.ui.custom.asyncimage.ImageCache;
import io.xeres.ui.event.OpenUriEvent;
import io.xeres.ui.event.UnreadEvent;
import io.xeres.ui.model.board.BoardGroup;
import io.xeres.ui.model.board.BoardMapper;
import io.xeres.ui.model.board.BoardMessage;
import io.xeres.ui.support.clipboard.ClipboardUtils;
import io.xeres.ui.support.contentline.Content;
import io.xeres.ui.support.loader.OnDemandLoader;
import io.xeres.ui.support.loader.OnDemandLoaderAction;
import io.xeres.ui.support.markdown.MarkdownService;
import io.xeres.ui.support.unread.UnreadService;
import io.xeres.ui.support.uri.BoardUri;
import io.xeres.ui.support.util.DateUtils;
import io.xeres.ui.support.util.UiUtils;
import io.xeres.ui.support.window.WindowManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.SplitPane;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import net.rgielen.fxweaver.core.FxmlView;
import org.fxmisc.flowless.VirtualFlow;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;

import java.util.*;

import static io.xeres.common.rest.PathConfig.BOARDS_PATH;
import static io.xeres.ui.support.preference.PreferenceUtils.BOARDS;
import static javafx.scene.control.Alert.AlertType.WARNING;

@Component
@FxmlView(value = "/view/board/board_view.fxml")
public class BoardViewController implements Controller, GxsGroupTreeTableAction<BoardGroup>, OnDemandLoaderAction<BoardGroup>
{
	private final WindowManager windowManager;

	@FXML
	private GxsGroupTreeTableView<BoardGroup> boardTree;

	@FXML
	private SplitPane splitPaneVertical;

	@FXML
	private Button createBoard;

	@FXML
	private Button newPost;

	@FXML
	private StackPane contentGroup;

	private InfoView infoView;

	private final ObservableList<BoardMessage> messages = FXCollections.observableArrayList();

	private OnDemandLoader<BoardGroup, BoardMessage> onDemandLoader;

	private VirtualFlow<BoardMessage, BoardMessageCell> virtualFlow;

	private final ResourceBundle bundle;

	private final BoardClient boardClient;
	private final NotificationClient notificationClient;
	private final GeneralClient generalClient;
	private final ImageCache imageCacheService;
	private final UnreadService unreadService;
	private final MarkdownService markdownService;
	private final ImageCache imageCache;

	private Disposable notificationDisposable;

	private UrlToOpen urlToOpen;

	public BoardViewController(BoardClient boardClient, ResourceBundle bundle, NotificationClient notificationClient, GeneralClient generalClient, ImageCache imageCacheService, UnreadService unreadService, MarkdownService markdownService, WindowManager windowManager, ImageCache imageCache)
	{
		this.boardClient = boardClient;
		this.bundle = bundle;

		this.notificationClient = notificationClient;
		this.generalClient = generalClient;
		this.imageCacheService = imageCacheService;
		this.unreadService = unreadService;
		this.markdownService = markdownService;
		this.windowManager = windowManager;
		this.imageCache = imageCache;
	}


	@Override
	public void initialize()
	{
		boardTree.initialize(BOARDS,
				boardClient,
				BoardGroup::new,
				() -> new BoardGroupCell(generalClient, imageCacheService),
				this);

		boardTree.unreadProperty().addListener((_, _, newValue) -> unreadService.sendUnreadEvent(UnreadEvent.Element.BOARD, newValue));

		// VirtualizedScrollPane doesn't work from FXML so we add it manually
		virtualFlow = VirtualFlow.createVertical(messages, boardMessage -> new BoardMessageCell(boardMessage, generalClient, boardClient, markdownService));
		VirtualizedScrollPane<VirtualFlow<BoardMessage, BoardMessageCell>> messagesView = new VirtualizedScrollPane<>(virtualFlow);
		VBox.setVgrow(messagesView, Priority.ALWAYS);
		contentGroup.getChildren().add(messagesView);

		// Create InfoView to display group info
		infoView = new InfoView();
		infoView.setLoader(url -> generalClient.getImage(url).block());
		contentGroup.getChildren().add(infoView);
		infoView.setVisible(false);

		onDemandLoader = new OnDemandLoader<>(messagesView, messages, boardClient, this);

		// The default handler is a bit slow, let's speed up
		// mouse scrolling.
		messagesView.addEventFilter(ScrollEvent.ANY, se -> {
			messagesView.scrollXBy(-se.getDeltaX());
			messagesView.scrollYBy(-se.getDeltaY() * 4);
			se.consume();
		});

		createBoard.setOnAction(_ -> windowManager.openBoardCreation(0L));

		newPost.setOnAction(_ -> newBoardPost());

		setupBoardNotifications();
	}

	@EventListener
	public void handleOpenUriEvent(OpenUriEvent event)
	{
		if (event.uri() instanceof BoardUri boardUri)
		{
			if (!boardTree.openUrl(boardUri.gxsId(), boardUri.msgId()))
			{
				UiUtils.showAlert(WARNING, bundle.getString("board.view.group.not-found"));
			}
		}
	}

	@Override
	public void onSubscribeToGroup(BoardGroup group)
	{

	}

	@Override
	public void onUnsubscribeFromGroup(BoardGroup group)
	{

	}

	@Override
	public void onCopyGroupLink(BoardGroup group)
	{
		var boardUri = new BoardUri(group.getName(), group.getGxsId(), null);
		ClipboardUtils.copyTextToClipboard(boardUri.toUriString());
	}

	@Override
	public void onOpenUrl(GxsId gxsId, MsgId msgId)
	{
		if (gxsId.equals(boardTree.getSelectedGroupGxsId()))
		{
			selectMessage(msgId);
		}
		else
		{
			urlToOpen = new UrlToOpen(gxsId, msgId);
		}
	}

	private void selectMessage(MsgId msgId)
	{
		for (var i = 0; i < messages.size(); i++)
		{
			var message = messages.get(i);
			if (message.getMsgId().equals(msgId))
			{
				virtualFlow.show(i);
				break;
			}
		}
	}

	@Override
	public void onSelectSubscribedGroup(BoardGroup group)
	{
		onDemandLoader.changeSelection(group);
		newPost.setDisable(false);
		showGroupInfo(null);
	}

	@Override
	public void onSelectUnsubscribedGroup(BoardGroup group)
	{
		onDemandLoader.changeSelection(group);
		newPost.setDisable(true);
		showGroupInfo(group);
	}

	@Override
	public void onUnselectGroup()
	{
		onDemandLoader.changeSelection(null);
		newPost.setDisable(true);
		showGroupInfo(null);
	}

	@Override
	public void onEditGroup(BoardGroup group)
	{
		windowManager.openBoardCreation(group.getId());
	}

	@EventListener
	public void onApplicationEvent(ContextClosedEvent ignored)
	{
		if (notificationDisposable != null && !notificationDisposable.isDisposed())
		{
			notificationDisposable.dispose();
		}
	}

	private void setupBoardNotifications()
	{
		notificationDisposable = notificationClient.getBoardNotifications()
				.doOnError(UiUtils::webAlertError)
				.doOnNext(sse -> Platform.runLater(() -> {
					switch (sse.data())
					{
						case AddOrUpdateBoardGroups action ->
						{
							action.boardGroups().forEach(boardGroupItem -> imageCache.evictImage(RemoteUtils.getControlUrl() + BOARDS_PATH + "/groups/" + boardGroupItem.id() + "/image"));

							boardTree.addGroups(action.boardGroups().stream()
									.map(BoardMapper::fromDTO)
									.toList());
						}
						case AddOrUpdateBoardMessages action -> addBoardMessages(action.boardMessages().stream()
								.map(BoardMapper::fromDTO)
								.toList());
						case SetBoardMessageReadState action -> setMessageReadState(action.groupId(), action.messageId(), action.read());
						case SetBoardGroupMessagesReadState action -> setGroupMessagesReadState(action.groupId(), action.read());
						case null -> throw new IllegalArgumentException("Board notifications have not been set");
					}
				}))
				.subscribe();
	}

	private void setMessageReadState(long groupId, long messageId, boolean read)
	{
		onDemandLoader.setMessageReadState(groupId, messageId, read);
		boardTree.setUnreadCount(groupId, read);
	}

	private void setGroupMessagesReadState(long groupId, boolean read)
	{
		onDemandLoader.setGroupMessagesReadState(groupId, read);
		boardTree.refreshUnreadCount(groupId);
	}

	private void newBoardPost()
	{
		windowManager.openBoardMessage(boardTree.getSelectedGroupId());
	}

	private void addBoardMessages(List<BoardMessage> boardMessages)
	{
		Set<GxsId> boardsToUpdate = new HashSet<>();

		for (BoardMessage boardMessage : boardMessages)
		{
			onDemandLoader.insertMessage(boardMessage);
			boardsToUpdate.add(boardMessage.getGxsId());
		}
		boardTree.refreshUnreadCount(boardsToUpdate);
	}

	@Override
	public void onMessagesLoaded(BoardGroup group)
	{
		if (urlToOpen != null)
		{
			if (group.getGxsId().equals(urlToOpen.gxsId()))
			{
				selectMessage(urlToOpen.msgId());
				urlToOpen = null;
			}
		}
	}

	private List<Node> createContent(String input)
	{
		return markdownService.parse(input, EnumSet.noneOf(MarkdownService.Rendering.class)).stream()
				.map(Content::getNode).toList();
	}

	private void showGroupInfo(BoardGroup group)
	{
		if (group != null && group.isReal())
		{
			var header = createContent("""
					## %s
					
					%s: %s\\
					%s: %s
					""".formatted(
					group.getName(),
					bundle.getString("posts-at-remote-nodes"),
					group.getVisibleMessageCount(),
					bundle.getString("last-activity"),
					DateUtils.formatDateTime(group.getLastActivity(), bundle.getString("unknown-lc"))));

			var body = createContent(group.getDescription());

			if (group.hasImage())
			{
				infoView.setInfo(header, body, RemoteUtils.getControlUrl() + BOARDS_PATH + "/groups/" + group.getId() + "/image", GxsGroupConstants.IMAGE_SIDE_SIZE, GxsGroupConstants.IMAGE_SIDE_SIZE);
			}
			else
			{
				infoView.setInfo(header, body);
			}

			infoView.setVisible(true);
		}
		else
		{
			infoView.setInfo(null, null);
			infoView.setVisible(false);
		}
	}

	record UrlToOpen(GxsId gxsId, MsgId msgId)
	{

	}
}
