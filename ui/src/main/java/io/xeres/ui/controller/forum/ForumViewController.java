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
import io.xeres.common.id.MsgId;
import io.xeres.common.rest.forum.ForumPostRequest;
import io.xeres.common.rest.notification.forum.AddOrUpdateForumGroups;
import io.xeres.common.rest.notification.forum.AddOrUpdateForumMessages;
import io.xeres.common.rest.notification.forum.SetForumGroupMessagesReadState;
import io.xeres.common.rest.notification.forum.SetForumMessageReadState;
import io.xeres.ui.client.ForumClient;
import io.xeres.ui.client.GeneralClient;
import io.xeres.ui.client.IdentityClient;
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
import io.xeres.ui.support.loader.OnDemandLoader;
import io.xeres.ui.support.loader.OnDemandLoaderAction;
import io.xeres.ui.support.markdown.MarkdownService;
import io.xeres.ui.support.markdown.MarkdownService.Rendering;
import io.xeres.ui.support.unread.UnreadService;
import io.xeres.ui.support.uri.ForumUri;
import io.xeres.ui.support.uri.IdentityUri;
import io.xeres.ui.support.uri.UriService;
import io.xeres.ui.support.util.DateUtils;
import io.xeres.ui.support.util.TextFlowDragSelection;
import io.xeres.ui.support.util.UiUtils;
import io.xeres.ui.support.window.WindowManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.text.TextFlow;
import net.rgielen.fxweaver.core.FxmlView;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.*;

import static io.xeres.common.dto.identity.IdentityConstants.OWN_IDENTITY_ID;
import static io.xeres.ui.support.preference.PreferenceUtils.FORUMS;
import static io.xeres.ui.support.util.DateUtils.DATE_TIME_PRECISE_FORMAT;
import static javafx.scene.control.Alert.AlertType.WARNING;
import static javafx.scene.control.TreeTableColumn.SortType.DESCENDING;

@Component
@FxmlView(value = "/view/forum/forum_view.fxml")
public class ForumViewController implements Controller, GxsGroupTreeTableAction<ForumGroup>, OnDemandLoaderAction<ForumGroup>
{
	private static final Logger log = LoggerFactory.getLogger(ForumViewController.class);

	private static final String EDIT_FORUM_MESSAGE_MENU_ID = "editForumMessage";
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

	@FXML
	private ChoiceBox<MessageVersion> versionChoiceBox;

	private final ObservableList<ForumMessage> messages = FXCollections.observableArrayList();

	private OnDemandLoader<ForumGroup, ForumMessage> onDemandLoader;

	private final ObservableList<MessageVersion> versions = FXCollections.observableArrayList();

	private int versionsFetcherRun;

	private final ResourceBundle bundle;

	private final ForumClient forumClient;
	private final NotificationClient notificationClient;
	private final WindowManager windowManager;
	private final MarkdownService markdownService;
	private final UriService uriService;
	private final GeneralClient generalClient;
	private final ImageCache imageCacheService;
	private final UnreadService unreadService;
	private final IdentityClient identityClient;

	private ForumMessage selectedForumMessage;

	private Disposable notificationDisposable;

	private TreeItem<ForumMessage> forumMessagesRoot;

	private MsgId toSelectMsgId;
	private UrlToOpen urlToOpen;

	private GxsId ownIdentityGxsId;

	private final ChangeListener<MessageVersion> changeVersionListener = (_, _, messageVersion) -> {
		if (messageVersion != null)
		{
			changeSelectedForumMessageVersion(messageVersion.id());
		}
	};

	public ForumViewController(ForumClient forumClient, ResourceBundle bundle, NotificationClient notificationClient, WindowManager windowManager, MarkdownService markdownService, UriService uriService, GeneralClient generalClient, ImageCache imageCacheService, UnreadService unreadService, IdentityClient identityClient)
	{
		this.forumClient = forumClient;
		this.bundle = bundle;

		this.notificationClient = notificationClient;
		this.windowManager = windowManager;
		this.markdownService = markdownService;
		this.uriService = uriService;
		this.generalClient = generalClient;
		this.imageCacheService = imageCacheService;
		this.unreadService = unreadService;
		this.identityClient = identityClient;
	}

	@Override
	public void initialize()
	{
		log.debug("Trying to get forums list...");

		forumTree.initialize(FORUMS,
				forumClient,
				ForumGroup::new,
				ForumCell::new,
				this);

		forumTree.unreadProperty().addListener((_, _, newValue) -> unreadService.sendUnreadEvent(UnreadEvent.Element.FORUM, newValue));

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

		identityClient.findById(OWN_IDENTITY_ID)
				.doOnSuccess(identity -> Platform.runLater(() -> {
					assert identity != null;
					ownIdentityGxsId = identity.getGxsId();
				}))
				.subscribe();

		versionChoiceBox.setItems(versions);

		onDemandLoader = new OnDemandLoader<>(forumMessagesTreeTableView, messages, forumClient, this);

		createForum.setOnAction(_ -> windowManager.openForumCreation(0L));

		newThread.setOnAction(_ -> newForumPost(false));

		setupForumNotifications();

		TextFlowDragSelection.enableSelection(messageContent, messagePane);
	}

	@EventListener
	public void handleOpenUriEvent(OpenUriEvent event)
	{
		if (event.uri() instanceof ForumUri forumUri)
		{
			if (!forumTree.openUrl(forumUri.gxsId(), forumUri.msgId()))
			{
				UiUtils.showAlert(WARNING, bundle.getString("forum.view.group.not-found"));
			}
		}
	}

	@Override
	public void onOpenUrl(GxsId gxsId, MsgId msgId)
	{
		if (gxsId.equals(forumTree.getSelectedGroupGxsId()))
		{
			selectMessage(msgId);
		}
		else
		{
			urlToOpen = new UrlToOpen(gxsId, msgId);
		}
	}

	private void setMessageToSelect(MsgId msgId)
	{
		if (msgId != null)
		{
			toSelectMsgId = msgId;
		}
	}

	private void selectMessageIfNeeded()
	{
		if (toSelectMsgId != null)
		{
			forumMessagesRoot.getChildren().stream()
					.filter(forumMessageTreeItem -> forumMessageTreeItem.getValue().getMsgId().equals(toSelectMsgId))
					.findFirst()
					.ifPresent(forumMessageTreeItem -> Platform.runLater(() -> forumMessagesTreeTableView.getSelectionModel().select(forumMessageTreeItem)));
		}
	}

	private void selectMessage(MsgId msgId)
	{
		forumMessagesRoot.getChildren().stream()
				.filter(forumMessageTreeItem -> forumMessageTreeItem.getValue().getMsgId().equals(msgId))
				.findFirst()
				.ifPresentOrElse(forumMessageTreeItem -> Platform.runLater(() -> forumMessagesTreeTableView.getSelectionModel().select(forumMessageTreeItem)),
						() -> UiUtils.showAlert(WARNING, bundle.getString("forum.view.message.not-found")));
	}

	private void createForumMessageTableViewContextMenu()
	{
		var replyItem = new MenuItem(bundle.getString("forum.view.reply"));
		replyItem.setGraphic(new FontIcon(MaterialDesignR.REPLY));
		replyItem.setOnAction(_ -> newForumPost(true));

		var markUnreadItem = new MenuItem(bundle.getString("mark-unread"));
		markUnreadItem.setGraphic(new FontIcon(MaterialDesignE.EMAIL_MARK_AS_UNREAD));
		markUnreadItem.setOnAction(_ -> markAsUnread());

		var editItem = new MenuItem(bundle.getString("edit"));
		editItem.setId(EDIT_FORUM_MESSAGE_MENU_ID);
		editItem.setGraphic(new FontIcon(MaterialDesignS.SQUARE_EDIT_OUTLINE));
		editItem.setOnAction(_ -> editForumPost());

		var copyLinkItem = new MenuItem(bundle.getString("copy-link"));
		copyLinkItem.setId(COPY_LINK_MENU_ID);
		copyLinkItem.setGraphic(new FontIcon(MaterialDesignL.LINK_VARIANT));
		copyLinkItem.setOnAction(event -> {
			@SuppressWarnings("unchecked") var forumMessage = ((TreeItem<ForumMessage>) event.getSource()).getValue();
			var forumUri = new ForumUri(forumMessage.getName(), forumMessage.getGxsId(), forumMessage.getMsgId());
			ClipboardUtils.copyTextToClipboard(forumUri.toUriString());
		});

		var xContextMenu = new XContextMenu<TreeItem<ForumMessage>>(replyItem, markUnreadItem, editItem, new SeparatorMenuItem(), copyLinkItem);
		xContextMenu.setOnShowing((contextMenu, treeItem) -> {
			if (treeItem == null)
			{
				return false;
			}
			contextMenu.getItems().stream()
					.filter(menuItem -> EDIT_FORUM_MESSAGE_MENU_ID.equals(menuItem.getId()))
					.findFirst().ifPresent(menuItem -> menuItem.setVisible(treeItem.getValue().getAuthorGxsId() != null && treeItem.getValue().getAuthorGxsId().equals(ownIdentityGxsId)));
			return true;
		});
		xContextMenu.addToNode(forumMessagesTreeTableView);
	}

	private void newForumPost(boolean replyTo)
	{
		var replyToId = 0L;

		if (selectedForumMessage != null)
		{
			replyToId = replyTo ? selectedForumMessage.getId() : 0L;
		}

		var postRequest = new ForumPostRequest(forumTree.getSelectedGroupId(), replyToId, 0L);
		windowManager.openForumEditor(postRequest);
	}

	private void markAsUnread()
	{
		if (selectedForumMessage == null) // Should not happen
		{
			return;
		}

		forumClient.setForumMessageReadState(selectedForumMessage.getId(), false)
				.subscribe();
	}

	private void editForumPost()
	{
		if (selectedForumMessage != null)
		{
			var postRequest = new ForumPostRequest(forumTree.getSelectedGroupId(), 0L, selectedForumMessage.getId());
			windowManager.openForumEditor(postRequest);
		}
	}

	private void setupForumNotifications()
	{
		notificationDisposable = notificationClient.getForumNotifications()
				.doOnError(UiUtils::webAlertError)
				.doOnNext(sse -> Platform.runLater(() -> {
					switch (sse.data())
					{
						case AddOrUpdateForumGroups action -> forumTree.addGroups(action.forumGroups().stream()
								.map(ForumMapper::fromDTO)
								.toList());
						case AddOrUpdateForumMessages action -> addForumMessages(action.forumMessages().stream()
								.map(ForumMapper::fromDTO)
								.toList());
						case SetForumMessageReadState action -> setMessageReadState(action.groupId(), action.messageId(), action.read());
						case SetForumGroupMessagesReadState action -> setGroupMessagesReadState(action.groupId(), action.read());
						case null -> throw new IllegalArgumentException("Forum notifications have not been set");
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

	private void changeSelectedForumMessage(ForumMessage forumMessage)
	{
		selectedForumMessage = forumMessage;

		if (forumMessage != null)
		{
			forumClient.getForumMessage(forumMessage.getId())
					.doOnSuccess(message -> Platform.runLater(() -> {
						assert message != null;
						setCommonMessageAttributes(message);
						messageAuthor.setText(message.getAuthorName());
						createAuthorContextMenu(message.getAuthorName(), message.getAuthorGxsId());
						setupMessageVersionSelector(message);
						UiUtils.setPresent(messageHeader);
						if (!message.isRead())
						{
							forumClient.setForumMessageReadState(message.getId(), true)
									.subscribe();
						}
					}))
					.doOnError(UiUtils::webAlertError)
					.subscribe();
		}
		else
		{
			clearMessage();
		}
	}

	private void changeSelectedForumMessageVersion(long id)
	{
		if (selectedForumMessage != null)
		{
			forumClient.getForumMessage(id)
					.doOnSuccess(message -> Platform.runLater(() -> {
						assert message != null;
						setCommonMessageAttributes(message);
					}))
					.doOnError(UiUtils::webAlertError)
					.subscribe();
		}
	}

	private void setCommonMessageAttributes(ForumMessage forumMessage)
	{
		messageContent.getChildren().clear();
		messagePane.setVvalue(messagePane.getVmin()); // Reset scroll position
		addMessageContent(forumMessage.getContent());
		messageDate.setText(DATE_TIME_PRECISE_FORMAT.format(forumMessage.getPublished()));
		messageSubject.setText(forumMessage.getName());
	}

	private void setupMessageVersionSelector(ForumMessage forumMessage)
	{
		versionChoiceBox.getSelectionModel().selectedItemProperty().removeListener(changeVersionListener); // Prevent listener from kicking in while we fill and select entries

		versionChoiceBox.setVisible(forumMessage.getOriginalId() != 0L);
		versions.clear();
		versions.addFirst(new MessageVersion(null, forumMessage.getId()));
		versionChoiceBox.getSelectionModel().selectFirst();

		versionChoiceBox.getSelectionModel().selectedItemProperty().addListener(changeVersionListener);

		if (forumMessage.getOriginalId() != 0L)
		{
			fetchVersions(forumMessage.getOriginalId(), ++versionsFetcherRun, 0);
		}
	}

	private void fetchVersions(long id, int run, int recursion)
	{
		if (versionsFetcherRun != run)
		{
			return;
		}

		forumClient.getForumMessage(id)
				.publishOn(Schedulers.boundedElastic())
				.doOnSuccess(message -> Platform.runLater(() -> {
					assert message != null;

					versions.add(new MessageVersion(message.getPublished(), message.getId()));

					if (message.getOriginalId() != 0L && recursion < 16)
					{
						fetchVersions(message.getOriginalId(), run, recursion + 1);
					}
				}))
				.subscribe();
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
		Set<GxsId> forumsToUpdate = new HashSet<>();

		for (ForumMessage forumMessage : forumMessages)
		{
			onDemandLoader.insertMessage(forumMessage);
			forumsToUpdate.add(forumMessage.getGxsId());
		}
		forumTree.refreshUnreadCount(forumsToUpdate);
		refreshMessageList();
	}

	private void setMessageReadState(long groupId, long messageId, boolean read)
	{
		// Avoids flickering because of some current Flowless limitation
		if (selectedForumMessage != null && selectedForumMessage.getId() == messageId && !selectedForumMessage.isRead())
		{
			forumTree.setUnreadCount(groupId, read);
			selectedForumMessage.setRead(read);
			forumMessagesTreeTableView.refresh();
		}
	}

	private void setGroupMessagesReadState(long groupId, boolean read)
	{
		onDemandLoader.setGroupMessagesReadState(groupId, read);
		forumTree.refreshUnreadCount(groupId);
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
	public void onSubscribeToGroup(ForumGroup group)
	{
	}

	@Override
	public void onUnsubscribeFromGroup(ForumGroup group)
	{
	}

	@Override
	public void onCopyGroupLink(ForumGroup group)
	{
		var forumUri = new ForumUri(group.getName(), group.getGxsId(), null);
		ClipboardUtils.copyTextToClipboard(forumUri.toUriString());
	}

	@Override
	public void onSelectSubscribedGroup(ForumGroup group)
	{
		showInfo(group);
		forumMessagesState(true);
		onDemandLoader.changeSelection(group);
		newThread.setDisable(false);
	}

	private void saveSelection()
	{
		var selectedItem = forumMessagesTreeTableView.getSelectionModel().getSelectedItem();
		if (selectedItem != null)
		{
			setMessageToSelect(selectedItem.getValue().getMsgId());
			forumMessagesTreeTableView.getSelectionModel().clearSelection();
		}
	}

	private void refreshMessageList()
	{
		saveSelection();
		forumMessagesRoot.getChildren().clear();
		forumMessagesRoot.getChildren().addAll(toTreeItemForumMessages(messages));
		forumMessagesTreeTableView.sort();
		newThread.setDisable(false);
		selectMessageIfNeeded();

		forumMessagesState(false);
	}

	@Override
	public void onSelectUnsubscribedGroup(ForumGroup group)
	{
		onDemandLoader.changeSelection(group);
		newThread.setDisable(true);
		showInfo(group);
	}

	@Override
	public void onUnselectGroup()
	{
		onDemandLoader.changeSelection(null);
		newThread.setDisable(true);
		showInfo(null);
	}

	@Override
	public void onEditGroup(ForumGroup group)
	{
		windowManager.openForumCreation(group.getId());
	}

	private void showInfo(ForumGroup group)
	{
		selectedForumMessage = null;

		forumMessagesTreeTableView.getSelectionModel().clearSelection();
		forumMessagesRoot.getChildren().clear();
		clearMessage();
		if (group != null && group.isReal())
		{
			addMessageContent("""
					## %s
					
					%s
					
					%s: %s\\
					%s: %s
					""".formatted(
					group.getName(),
					group.getDescription(),
					bundle.getString("posts-at-remote-nodes"),
					group.getVisibleMessageCount(),
					bundle.getString("last-activity"),
					DateUtils.formatDateTime(group.getLastActivity(), bundle.getString("unknown-lc"))
			));
		}
		forumMessagesState(false);
		toSelectMsgId = null;
	}

	private void addMessageContent(String input)
	{
		messageContent.getChildren().addAll(markdownService.parse(input, EnumSet.noneOf(Rendering.class)).stream()
				.map(Content::getNode).toList());
	}

	@Override
	public void onMessagesLoaded(ForumGroup group)
	{
		refreshMessageList();
		if (urlToOpen != null)
		{
			if (group.getGxsId().equals(urlToOpen.gxsId()))
			{
				selectMessage(urlToOpen.msgId());
				urlToOpen = null;
			}
		}
	}

	record UrlToOpen(GxsId gxsId, MsgId msgId)
	{

	}
}
