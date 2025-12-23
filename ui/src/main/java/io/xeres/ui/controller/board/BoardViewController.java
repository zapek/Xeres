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

import io.xeres.common.id.GxsId;
import io.xeres.common.rest.notification.board.AddBoardMessages;
import io.xeres.common.rest.notification.board.AddOrUpdateBoardGroups;
import io.xeres.common.rest.notification.board.MarkBoardMessagesAsRead;
import io.xeres.ui.client.BoardClient;
import io.xeres.ui.client.GeneralClient;
import io.xeres.ui.client.NotificationClient;
import io.xeres.ui.controller.Controller;
import io.xeres.ui.controller.common.GxsGroupTreeTableAction;
import io.xeres.ui.controller.common.GxsGroupTreeTableView;
import io.xeres.ui.custom.asyncimage.ImageCache;
import io.xeres.ui.event.UnreadEvent;
import io.xeres.ui.model.board.BoardGroup;
import io.xeres.ui.model.board.BoardMapper;
import io.xeres.ui.model.board.BoardMessage;
import io.xeres.ui.support.markdown.MarkdownService;
import io.xeres.ui.support.unread.UnreadService;
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

import static io.xeres.ui.support.preference.PreferenceUtils.BOARDS;

@Component
@FxmlView(value = "/view/board/board_view.fxml")
public class BoardViewController implements Controller, GxsGroupTreeTableAction<BoardGroup>
{
	private static final Logger log = LoggerFactory.getLogger(BoardViewController.class);

	@FXML
	private GxsGroupTreeTableView<BoardGroup> boardTree;

	@FXML
	private SplitPane splitPaneVertical;

	@FXML
	private Button newBoard;

	@FXML
	private StackPane contentGroup;

	private final ObservableList<BoardMessage> messages = FXCollections.observableArrayList();

	VirtualizedScrollPane<VirtualFlow<BoardMessage, BoardMessageCell>> messagesView;

	private final ResourceBundle bundle;

	private final BoardClient boardClient;
	private final NotificationClient notificationClient;
	private final GeneralClient generalClient;
	private final ImageCache imageCacheService;
	private final UnreadService unreadService;
	private final JsonMapper jsonMapper;
	private final MarkdownService markdownService;

	private Disposable notificationDisposable;

	public BoardViewController(BoardClient boardClient, ResourceBundle bundle, NotificationClient notificationClient, GeneralClient generalClient, ImageCache imageCacheService, UnreadService unreadService, JsonMapper jsonMapper, MarkdownService markdownService)
	{
		this.boardClient = boardClient;
		this.bundle = bundle;

		this.notificationClient = notificationClient;
		this.generalClient = generalClient;
		this.imageCacheService = imageCacheService;
		this.unreadService = unreadService;
		this.jsonMapper = jsonMapper;
		this.markdownService = markdownService;
	}


	@Override
	public void initialize()
	{
		log.debug("Trying to get boards list...");
		boardTree.initialize(BOARDS,
				boardClient,
				BoardGroup::new,
				() -> new BoardGroupCell(generalClient, imageCacheService),
				this,
				hasUnreadMessages -> unreadService.sendUnreadEvent(UnreadEvent.Element.BOARD, hasUnreadMessages)
		);

		// XXX: add the rest...

		messagesView = createMessageView();
		VBox.setVgrow(messagesView, Priority.ALWAYS);
		contentGroup.getChildren().add(messagesView);

		// XXX: dynamic loading can be done with the following:
//		messagesView.getContent().needsLayoutProperty().addListener((observable, oldValue, newValue) -> {
//			if (newValue)
//			{
//				log.debug("layout, first index: {}, last index: {}", messagesView.getContent().getFirstVisibleIndex(), messagesView.getContent().getLastVisibleIndex());
//			}
//		});

		setupBoardNotifications();
	}

	private VirtualizedScrollPane<VirtualFlow<BoardMessage, BoardMessageCell>> createMessageView()
	{
		// VirtualizedScrollPane doesn't work from FXML so we add it manually
		var view = VirtualFlow.createVertical(messages, boardMessage -> new BoardMessageCell(boardMessage, generalClient, markdownService));
		return new VirtualizedScrollPane<>(view);
	}

	@Override
	public void onSubscribe(BoardGroup group)
	{

	}

	@Override
	public void onUnsubscribe(BoardGroup group)
	{

	}

	@Override
	public void onCopyLink(BoardGroup group)
	{

	}

	@Override
	public void onSelectSubscribed(BoardGroup group)
	{
		boardClient.getBoardMessages(group.getId()).collectList()
				// XXX: progress bar too?
				.doOnSuccess(receivedMessages -> Platform.runLater(() -> {
					messages.clear();
					messages.addAll(receivedMessages.stream().sorted(Comparator.comparing(BoardMessage::getPublished).reversed()).toList());
					messagesView.getContent().showAsFirst(0);
				}))
				.doOnError(UiUtils::showAlertError) // XXX: cleanup on error?
				// XXX: finally state?
				.subscribe();
	}

	@Override
	public void onSelectUnsubscribed(BoardGroup group)
	{
		messages.clear();
	}

	@Override
	public void onUnselect()
	{

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
				.doOnError(UiUtils::showAlertError)
				.doOnNext(sse -> Platform.runLater(() -> {
					if (sse.data() != null)
					{
						var idName = Objects.requireNonNull(sse.id());

						if (idName.equals(AddOrUpdateBoardGroups.class.getSimpleName()))
						{
							var action = jsonMapper.convertValue(sse.data().action(), AddOrUpdateBoardGroups.class);

							boardTree.addGroups(action.boardGroups().stream()
									.map(BoardMapper::fromDTO)
									.toList());
						}
						else if (idName.equals(AddBoardMessages.class.getSimpleName()))
						{
							var action = jsonMapper.convertValue(sse.data().action(), AddBoardMessages.class);

							addBoardMessages(action.boardMessages().stream()
									.map(BoardMapper::fromDTO)
									.toList());
						}
						else if (idName.equals(MarkBoardMessagesAsRead.class.getSimpleName()))
						{
							var action = jsonMapper.convertValue(sse.data().action(), MarkBoardMessagesAsRead.class);

							markBoardMessagesAsRead(action.messageMap());
						}
						else
						{
							log.debug("Unknown board notification");
						}
					}
				}))
				.subscribe();
	}

	private void markBoardMessagesAsRead(Map<Long, Boolean> messageMap)
	{
		// Handle the most common case quickly
		if (messageMap.size() == 1)
		{
			var message = messageMap.entrySet().iterator().next();
			// XXX: implement somehow?
		}

		messageMap.forEach((_, _) -> {
			// XXX: implement... boring. not needed yet because we can't mark several entries at once
		});
	}

	private void addBoardMessages(List<BoardMessage> boardMessages)
	{
		Map<GxsId, Integer> boardsToSetCount = new HashMap<>();
		var needsSorting = false;
		var selectedBoardGroup = boardTree.getSelectedGroup();

		for (BoardMessage boardMessage : boardMessages)
		{
			if (selectedBoardGroup != null && boardMessage.getGxsId().equals(selectedBoardGroup.getGxsId()))
			{
				add(boardMessage);
				needsSorting = true;
			}
			boardsToSetCount.merge(boardMessage.getGxsId(), 1, Integer::sum);
		}

		if (needsSorting)
		{
			messages.sort(Comparator.comparing(BoardMessage::getPublished).reversed());
		}
		boardTree.addUnreadCount(boardsToSetCount);
	}

	private void add(BoardMessage boardMessage)
	{
		messages.addFirst(boardMessage);
	}
}
