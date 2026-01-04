/*
 * Copyright (c) 2023-2026 by David Gerber - https://zapek.com
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

package io.xeres.ui.controller.forum;

import io.xeres.common.id.GxsId;
import io.xeres.common.id.MessageId;
import io.xeres.common.rest.forum.ForumPostRequest;
import io.xeres.common.rest.notification.forum.AddForumGroups;
import io.xeres.common.rest.notification.forum.AddForumMessages;
import io.xeres.common.rest.notification.forum.MarkForumMessagesAsRead;
import io.xeres.ui.client.ForumClient;
import io.xeres.ui.client.GeneralClient;
import io.xeres.ui.client.NotificationClient;
import io.xeres.ui.controller.Controller;
import io.xeres.ui.controller.common.GxsGroupTreeTableAction;
import io.xeres.ui.controller.common.GxsGroupTreeTableView;
import io.xeres.ui.custom.ProgressPane;
import io.xeres.ui.custom.asyncimage.ImageCache;
import io.xeres.ui.event.OpenUriEvent;
import io.xeres.ui.event.UnreadEvent;
import io.xeres.ui.model.forum.ForumGroup;
import io.xeres.ui.model.forum.ForumMapper;
import io.xeres.ui.model.forum.ForumMessage;
import io.xeres.ui.support.clipboard.ClipboardUtils;
import io.xeres.ui.support.contentline.Content;
import io.xeres.ui.support.contextmenu.XContextMenu;
import io.xeres.ui.support.markdown.MarkdownService;
import io.xeres.ui.support.markdown.MarkdownService.ParsingMode;
import io.xeres.ui.support.unread.UnreadService;
import io.xeres.ui.support.uri.ForumUri;
import io.xeres.ui.support.uri.IdentityUri;
import io.xeres.ui.support.uri.UriService;
import io.xeres.ui.support.util.TextFlowDragSelection;
import io.xeres.ui.support.util.UiUtils;
import io.xeres.ui.support.window.WindowManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.text.TextFlow;
import net.rgielen.fxweaver.core.FxmlView;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignL;
import org.kordamp.ikonli.materialdesign2.MaterialDesignR;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.*;

import static io.xeres.ui.support.preference.PreferenceUtils.FORUMS;
import static io.xeres.ui.support.util.DateUtils.DATE_TIME_PRECISE_DISPLAY;
import static javafx.scene.control.Alert.AlertType.WARNING;
import static javafx.scene.control.TreeTableColumn.SortType.DESCENDING;

@Component
@FxmlView(value = "/view/forum/forum_view.fxml")
public class ForumViewController implements Controller, GxsGroupTreeTableAction<ForumGroup>
{
	private static final Logger log = LoggerFactory.getLogger(ForumViewController.class);

	private static final String COPY_LINK_MENU_ID = "copyLink";

	@FXML
	private GxsGroupTreeTableView<ForumGroup> forumTree;

	@FXML
	private SplitPane splitPaneVertical;

	@FXML
	private SplitPane splitPaneHorizontal;

	@FXML
	private TreeTableView<ForumMessage> forumMessagesTreeTableView;

	@FXML
	private TreeTableColumn<ForumMessage, String> treeTableSubject;

	@FXML
	private TreeTableColumn<ForumMessage, ForumMessage> treeTableAuthor;

	@FXML
	private TreeTableColumn<ForumMessage, Instant> treeTableDate;

	@FXML
	private ProgressPane forumMessagesProgress;

	@FXML
	private ScrollPane messagePane;

	@FXML
	private TextFlow messageContent;

	@FXML
	public Button createForum;

	@FXML
	private Button newThread;

	@FXML
	private GridPane messageHeader;

	@FXML
	private Label messageAuthor;

	@FXML
	private Label messageDate;

	@FXML
	private Label messageSubject;

	private final ResourceBundle bundle;

	private final ForumClient forumClient;
	private final NotificationClient notificationClient;
	private final WindowManager windowManager;
	private final JsonMapper jsonMapper;
	private final MarkdownService markdownService;
	private final UriService uriService;
	private final GeneralClient generalClient;
	private final ImageCache imageCacheService;
	private final UnreadService unreadService;

	private ForumMessage selectedForumMessage;

	private Disposable notificationDisposable;

	private TreeItem<ForumMessage> forumMessagesRoot;

	private MessageId messageIdToSelect;

	public ForumViewController(ForumClient forumClient, ResourceBundle bundle, NotificationClient notificationClient, WindowManager windowManager, JsonMapper jsonMapper, MarkdownService markdownService, UriService uriService, GeneralClient generalClient, ImageCache imageCacheService, UnreadService unreadService)
	{
		this.forumClient = forumClient;
		this.bundle = bundle;

		this.notificationClient = notificationClient;
		this.windowManager = windowManager;
		this.jsonMapper = jsonMapper;
		this.markdownService = markdownService;
		this.uriService = uriService;
		this.generalClient = generalClient;
		this.imageCacheService = imageCacheService;
		this.unreadService = unreadService;
	}

	@Override
	public void initialize()
	{
		log.debug("Trying to get forums list...");

		forumTree.initialize(FORUMS,
				forumClient,
				ForumGroup::new,
				ForumCell::new,
				this,
				hasUnreadMessages -> unreadService.sendUnreadEvent(UnreadEvent.Element.FORUM, hasUnreadMessages)
		);

		forumMessagesTreeTableView.setRowFactory(_ -> new ForumMessageCell());
		createForumMessageTableViewContextMenu();
		treeTableSubject.setCellValueFactory(new TreeItemPropertyValueFactory<>("name"));
		treeTableAuthor.setCellFactory(_ -> new ForumCellAuthor(generalClient, imageCacheService));
		treeTableAuthor.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getValue()));

		treeTableDate.setCellFactory(_ -> new DateCell());
		treeTableDate.setCellValueFactory(new TreeItemPropertyValueFactory<>("published"));

		forumMessagesRoot = new TreeItem<>(new ForumMessage());
		forumMessagesTreeTableView.setRoot(forumMessagesRoot);
		forumMessagesTreeTableView.setShowRoot(false);

		forumMessagesTreeTableView.getSortOrder().add(treeTableDate);
		treeTableDate.setSortType(DESCENDING);
		treeTableDate.setSortable(true);

		forumMessagesTreeTableView.getSelectionModel().selectedItemProperty()
				.addListener((_, _, newValue) -> changeSelectedForumMessage(newValue != null ? newValue.getValue() : null));

		createForum.setOnAction(_ -> windowManager.openForumCreation());

		newThread.setOnAction(_ -> newForumPost(false));

		setupForumNotifications();

		TextFlowDragSelection.enableSelection(messageContent, messagePane);
	}

	@EventListener
	public void handleOpenUriEvent(OpenUriEvent event)
	{
		if (event.uri() instanceof ForumUri forumUri)
		{
			var group = forumUri.id();
			var message = forumUri.messageId();

			forumTree.getAllGroups()
					.filter(forumGroupTreeItem -> forumGroupTreeItem.getValue().getGxsId().equals(group))
					.findFirst()
					.ifPresentOrElse(forumGroupTreeItem -> {
						setMessageToSelect(message);
						Platform.runLater(() -> {
							if (forumGroupTreeItem.equals(forumTree.getSelectionModel().getSelectedItem()))
							{
								// We need to select the message now if we're already on the right group
								// because it won't be selected for us automatically.
								selectMessageIfNeeded();
							}
							else
							{
								forumTree.getSelectionModel().select(forumGroupTreeItem);
							}
						});
					}, () -> UiUtils.alert(WARNING, bundle.getString("forum.view.group.not-found")));
		}
	}

	private void setMessageToSelect(MessageId message)
	{
		if (message != null)
		{
			messageIdToSelect = message;
		}
	}

	private void selectMessageIfNeeded()
	{
		if (messageIdToSelect != null)
		{
			forumMessagesRoot.getChildren().stream()
					.filter(forumMessageTreeItem -> forumMessageTreeItem.getValue().getMessageId().equals(messageIdToSelect))
					.findFirst()
					.ifPresentOrElse(forumMessageTreeItem -> Platform.runLater(() -> forumMessagesTreeTableView.getSelectionModel().select(forumMessageTreeItem)),
							() -> UiUtils.alert(WARNING, bundle.getString("forum.view.message.not-found")));

			messageIdToSelect = null;
		}
	}

	private void createForumMessageTableViewContextMenu()
	{
		var replyItem = new MenuItem(bundle.getString("forum.view.reply"));
		replyItem.setGraphic(new FontIcon(MaterialDesignR.REPLY));
		replyItem.setOnAction(_ -> newForumPost(true));

		var copyLinkItem = new MenuItem(bundle.getString("copy-link"));
		copyLinkItem.setId(COPY_LINK_MENU_ID);
		copyLinkItem.setGraphic(new FontIcon(MaterialDesignL.LINK_VARIANT));
		copyLinkItem.setOnAction(event -> {
			@SuppressWarnings("unchecked") var forumMessage = ((TreeItem<ForumMessage>) event.getSource()).getValue();
			var forumUri = new ForumUri(forumMessage.getName(), forumMessage.getGxsId(), forumMessage.getMessageId());
			ClipboardUtils.copyTextToClipboard(forumUri.toUriString());
		});

		var xContextMenu = new XContextMenu<ForumMessage>(replyItem, new SeparatorMenuItem(), copyLinkItem);
		xContextMenu.addToNode(forumMessagesTreeTableView);
	}

	private void newForumPost(boolean replyTo)
	{
		var replyToId = 0L;
		var originalId = 0L;

		if (selectedForumMessage != null)
		{
			replyToId = replyTo ? selectedForumMessage.getId() : 0L;
			//originalId = selectedForumMessage.getOriginalId(); // XXX: means edit. if desired, set it to its current id
		}

		var postRequest = new ForumPostRequest(forumTree.getSelectedGroup().getId(), originalId, replyToId);
		windowManager.openForumEditor(postRequest);
	}

	private void setupForumNotifications()
	{
		notificationDisposable = notificationClient.getForumNotifications()
				.doOnError(UiUtils::showAlertError)
				.doOnNext(sse -> Platform.runLater(() -> {
					if (sse.data() != null)
					{
						var idName = Objects.requireNonNull(sse.id());

						if (idName.equals(AddForumGroups.class.getSimpleName()))
						{
							var action = jsonMapper.convertValue(sse.data().action(), AddForumGroups.class);

							forumTree.addGroups(action.forumGroups().stream()
									.map(ForumMapper::fromDTO)
									.toList());
						}
						else if (idName.equals(AddForumMessages.class.getSimpleName()))
						{
							var action = jsonMapper.convertValue(sse.data().action(), AddForumMessages.class);

							addForumMessages(action.forumMessages().stream()
									.map(ForumMapper::fromDTO)
									.toList());
						}
						else if (idName.equals(MarkForumMessagesAsRead.class.getSimpleName()))
						{
							var action = jsonMapper.convertValue(sse.data().action(), MarkForumMessagesAsRead.class);

							markForumMessagesAsRead(action.messageMap());
						}
						else
						{
							log.debug("Unknown forum notification");
						}
					}
				}))
				.subscribe();
	}

	private void forumMessagesState(boolean loading)
	{
		Platform.runLater(() -> forumMessagesProgress.showProgress(loading));
	}

	// XXX: implement threaded support for the 2 following methods.
	// if the message has a parentId, find it in the list then add the message to it.
	// could be slow if the list is big so find tricks to speed it up
	private List<TreeItem<ForumMessage>> toTreeItemForumMessages(List<ForumMessage> forumMessages)
	{
		return forumMessages.stream()
				.map(TreeItem::new)
				.toList();
	}

	private void add(ForumMessage forumMessage)
	{
		forumMessagesRoot.getChildren().add(new TreeItem<>(forumMessage));
	}

	private void changeSelectedForumMessage(ForumMessage forumMessage)
	{
		selectedForumMessage = forumMessage;

		if (forumMessage != null)
		{
			forumClient.getForumMessage(forumMessage.getId())
					.doOnSuccess(message -> Platform.runLater(() -> {
						messageContent.getChildren().clear();
						messagePane.setVvalue(messagePane.getVmin());
						addMessageContent(message.getContent());
						messageAuthor.setText(forumMessage.getAuthorName());
						createAuthorContextMenu(forumMessage.getAuthorName(), forumMessage.getAuthorId());
						messageDate.setText(DATE_TIME_PRECISE_DISPLAY.format(forumMessage.getPublished()));
						messageSubject.setText(forumMessage.getName());
						UiUtils.setPresent(messageHeader);
						forumClient.updateForumMessagesRead(Map.of(message.getId(), true))
								.subscribe();
					}))
					.doOnError(UiUtils::showAlertError)
					.subscribe();
		}
		else
		{
			clearMessage();
		}
	}

	private void clearMessage()
	{
		UiUtils.setAbsent(messageHeader);
		messageAuthor.setText(null);
		messageAuthor.setContextMenu(null);
		messageDate.setText(null);
		messageSubject.setText(null);
		messageContent.getChildren().clear();
	}

	private void addForumMessages(List<ForumMessage> forumMessages)
	{
		Map<GxsId, Integer> forumsToSetCount = new HashMap<>();
		var needsSorting = false;
		var selectedForumGroup = forumTree.getSelectedGroup();

		for (ForumMessage forumMessage : forumMessages)
		{
			if (selectedForumGroup != null && forumMessage.getGxsId().equals(selectedForumGroup.getGxsId()))
			{
				add(forumMessage);
				needsSorting = true;
			}
			forumsToSetCount.merge(forumMessage.getGxsId(), 1, Integer::sum);
		}

		if (needsSorting)
		{
			forumMessagesTreeTableView.sort();
		}
		forumTree.addUnreadCount(forumsToSetCount);
	}

	private void markForumMessagesAsRead(Map<Long, Boolean> messageMap)
	{
		// Handle the most common case quickly
		if (messageMap.size() == 1)
		{
			var message = messageMap.entrySet().iterator().next();
			if (selectedForumMessage != null && selectedForumMessage.getId() == message.getKey() && !selectedForumMessage.isRead())
			{
				selectedForumMessage.setRead(message.getValue());
				forumMessagesTreeTableView.refresh();
				forumTree.subtractUnreadCountFromSelected(1);
				return;
			}
		}

		messageMap.forEach((_, _) -> {
			// XXX: implement... boring. not needed yet because we can't mark several entries at once
		});
	}

	@EventListener
	public void onApplicationEvent(ContextClosedEvent ignored)
	{
		if (notificationDisposable != null && !notificationDisposable.isDisposed())
		{
			notificationDisposable.dispose();
		}
	}

	private void createAuthorContextMenu(String name, GxsId gxsId)
	{
		var infoItem = new MenuItem(bundle.getString("chat.room.user-menu"));
		infoItem.setGraphic(new FontIcon(MaterialDesignA.ACCOUNT_BOX));
		infoItem.setOnAction(_ -> uriService.openUri(new IdentityUri(name, gxsId, null)));
		messageAuthor.setContextMenu(new ContextMenu(infoItem));
	}

	@Override
	public void onSubscribe(ForumGroup group)
	{
	}

	@Override
	public void onUnsubscribe(ForumGroup group)
	{
	}

	@Override
	public void onCopyLink(ForumGroup group)
	{
		var forumUri = new ForumUri(group.getName(), group.getGxsId(), null);
		ClipboardUtils.copyTextToClipboard(forumUri.toUriString());
	}

	@Override
	public void onSelectSubscribed(ForumGroup group)
	{
		selectedForumMessage = null;

		forumClient.getForumMessages(group.getId()).collectList()
				.doFirst(() -> forumMessagesState(true))
				.doOnSuccess(forumMessages -> Platform.runLater(() -> {
					forumMessagesTreeTableView.getSelectionModel().clearSelection(); // Important! Clear the selection before clearing the content, otherwise the next sort() crashes
					forumMessagesRoot.getChildren().clear();
					forumMessagesRoot.getChildren().addAll(toTreeItemForumMessages(forumMessages));
					forumMessagesTreeTableView.sort();
					clearMessage();
					newThread.setDisable(false);
					selectMessageIfNeeded();
				}))
				.doOnError(UiUtils::showAlertError) // XXX: cleanup on error?
				.doFinally(_ -> forumMessagesState(false))
				.subscribe();
	}

	@Override
	public void onSelectUnsubscribed(ForumGroup group)
	{
		showInfo(group);
	}

	@Override
	public void onUnselect()
	{
		showInfo(null);
	}

	@Override
	public void onEdit(ForumGroup group)
	{
		// XXX: add group editor
	}

	private void showInfo(ForumGroup group)
	{
		selectedForumMessage = null;

		forumMessagesTreeTableView.getSelectionModel().clearSelection();
		forumMessagesRoot.getChildren().clear();
		clearMessage();
		if (group != null && group.isReal())
		{
			addMessageContent(String.format("""
							**%s** (%s)
							
							%s
							""",
					group.getName(),
					group.getGxsId(),
					group.getDescription()
			));
		}
		newThread.setDisable(true);
		forumMessagesState(false);
		messageIdToSelect = null;
	}

	private void addMessageContent(String input)
	{
		messageContent.getChildren().addAll(markdownService.parse(input, EnumSet.of(ParsingMode.PARAGRAPH)).stream()
				.map(Content::getNode).toList());
	}
}
