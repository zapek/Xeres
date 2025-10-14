/*
 * Copyright (c) 2023-2025 by David Gerber - https://zapek.com
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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.MessageId;
import io.xeres.common.rest.forum.PostRequest;
import io.xeres.common.rest.notification.forum.AddForumGroups;
import io.xeres.common.rest.notification.forum.AddForumMessages;
import io.xeres.common.rest.notification.forum.MarkForumMessagesAsRead;
import io.xeres.ui.client.ForumClient;
import io.xeres.ui.client.GeneralClient;
import io.xeres.ui.client.NotificationClient;
import io.xeres.ui.controller.Controller;
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
import io.xeres.ui.support.preference.PreferenceUtils;
import io.xeres.ui.support.unread.UnreadService;
import io.xeres.ui.support.uri.ForumUri;
import io.xeres.ui.support.uri.IdentityUri;
import io.xeres.ui.support.uri.UriService;
import io.xeres.ui.support.util.TextFlowDragSelection;
import io.xeres.ui.support.util.UiUtils;
import io.xeres.ui.support.window.WindowManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.input.*;
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

import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

import static io.xeres.ui.support.preference.PreferenceUtils.FORUMS;
import static io.xeres.ui.support.util.DateUtils.DATE_TIME_PRECISE_DISPLAY;
import static javafx.scene.control.Alert.AlertType.WARNING;
import static javafx.scene.control.TreeTableColumn.SortType.DESCENDING;

@Component
@FxmlView(value = "/view/forum/forumview.fxml")
public class ForumViewController implements Controller
{
	private static final Logger log = LoggerFactory.getLogger(ForumViewController.class);

	private static final KeyCodeCombination COPY_KEY = new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN);

	private static final String SUBSCRIBE_MENU_ID = "subscribe";
	private static final String UNSUBSCRIBE_MENU_ID = "unsubscribe";
	private static final String COPY_LINK_MENU_ID = "copyLink";

	private static final String OPEN_OWN = "OpenOwn";
	private static final String OPEN_SUBSCRIBED = "OpenSubscribed";
	private static final String OPEN_POPULAR = "OpenPopular";
	private static final String OPEN_OTHER = "OpenOther";

	@FXML
	private TreeTableView<ForumGroup> forumTree;

	@FXML
	private TreeTableColumn<ForumGroup, String> forumNameColumn;

	@FXML
	private TreeTableColumn<ForumGroup, Integer> forumCountColumn;

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
	private final ObjectMapper objectMapper;
	private final MarkdownService markdownService;
	private final UriService uriService;
	private final GeneralClient generalClient;
	private final ImageCache imageCacheService;
	private final UnreadService unreadService;

	private ForumGroup selectedForumGroup;
	private ForumMessage selectedForumMessage;

	private Disposable notificationDisposable;

	private TreeItem<ForumMessage> forumMessagesRoot;

	private final TreeItem<ForumGroup> ownForums;
	private final TreeItem<ForumGroup> subscribedForums;
	private final TreeItem<ForumGroup> popularForums;
	private final TreeItem<ForumGroup> otherForums;

	private MessageId messageIdToSelect;

	public ForumViewController(ForumClient forumClient, ResourceBundle bundle, NotificationClient notificationClient, WindowManager windowManager, ObjectMapper objectMapper, MarkdownService markdownService, UriService uriService, GeneralClient generalClient, ImageCache imageCacheService, UnreadService unreadService)
	{
		this.forumClient = forumClient;
		this.bundle = bundle;

		ownForums = new TreeItem<>(new ForumGroup(bundle.getString("forum.tree.own")));
		subscribedForums = new TreeItem<>(new ForumGroup(bundle.getString("forum.tree.subscribed")));
		popularForums = new TreeItem<>(new ForumGroup(bundle.getString("forum.tree.popular")));
		otherForums = new TreeItem<>(new ForumGroup(bundle.getString("forum.tree.other")));
		this.notificationClient = notificationClient;
		this.windowManager = windowManager;
		this.objectMapper = objectMapper;
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

		var root = new TreeItem<>(new ForumGroup(""));
		//noinspection unchecked
		root.getChildren().addAll(ownForums, subscribedForums, popularForums, otherForums);
		root.setExpanded(true);
		forumTree.setRoot(root);
		forumTree.setShowRoot(false);
		forumTree.setRowFactory(_ -> new ForumCell());
		forumNameColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("name"));
		forumCountColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("unreadCount"));
		forumCountColumn.setCellFactory(_ -> new ForumCellCount());
		createForumTreeContextMenu();

		// We need Platform.runLater() because when an entry is moved, the selection can change
		forumTree.getSelectionModel().selectedItemProperty()
				.addListener((_, _, newValue) -> Platform.runLater(() -> changeSelectedForumGroup(newValue.getValue())));

		UiUtils.setOnPrimaryMouseDoubleClicked(forumTree, _ -> {
			if (isForumSelected())
			{
				subscribeToForumGroup(selectedForumGroup);
			}
		});

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

		getForumGroups();

		setupTrees();

		setupDragSelection();
	}

	private void setupDragSelection()
	{
		var selection = new TextFlowDragSelection(messageContent);
		messageContent.addEventFilter(MouseEvent.MOUSE_PRESSED, selection::press);
		messageContent.addEventFilter(MouseEvent.MOUSE_DRAGGED, selection::drag);
		messageContent.addEventFilter(MouseEvent.MOUSE_RELEASED, selection::release);
		messagePane.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
			if (COPY_KEY.match(event))
			{
				selection.copy();
				event.consume();
			}
		});
	}

	private void setupTrees()
	{
		var node = PreferenceUtils.getPreferences().node(FORUMS);
		ownForums.setExpanded(node.getBoolean(OPEN_OWN, false));
		subscribedForums.setExpanded(node.getBoolean(OPEN_SUBSCRIBED, false));
		popularForums.setExpanded(node.getBoolean(OPEN_POPULAR, false));
		otherForums.setExpanded(node.getBoolean(OPEN_OTHER, false));

		ownForums.expandedProperty().addListener((_, _, newValue) -> node.putBoolean(OPEN_OWN, newValue));
		subscribedForums.expandedProperty().addListener((_, _, newValue) -> node.putBoolean(OPEN_SUBSCRIBED, newValue));
		popularForums.expandedProperty().addListener((_, _, newValue) -> node.putBoolean(OPEN_POPULAR, newValue));
		otherForums.expandedProperty().addListener((_, _, newValue) -> node.putBoolean(OPEN_OTHER, newValue));
	}

	@EventListener
	public void handleOpenUriEvent(OpenUriEvent event)
	{
		if (event.uri() instanceof ForumUri forumUri)
		{
			var group = forumUri.id();
			var message = forumUri.messageId();

			Stream.concat(Stream.concat(Stream.concat(ownForums.getChildren().stream(), subscribedForums.getChildren().stream()), popularForums.getChildren().stream()), otherForums.getChildren().stream())
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

	private void createForumTreeContextMenu()
	{
		var subscribeItem = new MenuItem(bundle.getString("forum.tree.subscribe"));
		subscribeItem.setId(SUBSCRIBE_MENU_ID);
		subscribeItem.setGraphic(new FontIcon(MaterialDesignL.LOCATION_ENTER));
		//noinspection unchecked
		subscribeItem.setOnAction(event -> subscribeToForumGroup(((TreeItem<ForumGroup>) event.getSource()).getValue()));

		var unsubscribeItem = new MenuItem(bundle.getString("forum.tree.unsubscribe"));
		unsubscribeItem.setId(UNSUBSCRIBE_MENU_ID);
		unsubscribeItem.setGraphic(new FontIcon(MaterialDesignL.LOCATION_EXIT));
		//noinspection unchecked
		unsubscribeItem.setOnAction(event -> unsubscribeFromForumGroups(((TreeItem<ForumGroup>) event.getSource()).getValue()));

		var copyLinkItem = new MenuItem(bundle.getString("copy-link"));
		copyLinkItem.setId(COPY_LINK_MENU_ID);
		copyLinkItem.setGraphic(new FontIcon(MaterialDesignL.LINK_VARIANT));
		copyLinkItem.setOnAction(event -> {
			@SuppressWarnings("unchecked") var forumGroup = ((TreeItem<ForumGroup>) event.getSource()).getValue();
			var forumUri = new ForumUri(forumGroup.getName(), forumGroup.getGxsId(), null);
			ClipboardUtils.copyTextToClipboard(forumUri.toUriString());
		});

		var xContextMenu = new XContextMenu<TreeItem<ForumGroup>>(subscribeItem, unsubscribeItem, new SeparatorMenuItem(), copyLinkItem);
		xContextMenu.addToNode(forumTree);
		xContextMenu.setOnShowing((contextMenu, treeItem) -> {
			if (treeItem == null)
			{
				return false;
			}
			contextMenu.getItems().stream()
					.filter(menuItem -> SUBSCRIBE_MENU_ID.equals(menuItem.getId()))
					.findFirst().ifPresent(menuItem -> menuItem.setDisable(treeItem.getValue().isSubscribed()));

			contextMenu.getItems().stream()
					.filter(menuItem -> UNSUBSCRIBE_MENU_ID.equals(menuItem.getId()))
					.findFirst().ifPresent(menuItem -> menuItem.setDisable(!treeItem.getValue().isSubscribed()));

			return treeItem.getValue().isReal() && treeItem.getValue().isExternal();
		});
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

		var postRequest = new PostRequest(selectedForumGroup.getId(), originalId, replyToId);
		windowManager.openForumEditor(postRequest);
	}

	private boolean isForumSelected()
	{
		return selectedForumGroup != null && selectedForumGroup.isReal();
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
							var action = objectMapper.convertValue(sse.data().action(), AddForumGroups.class);

							addForumGroups(action.forumGroups().stream()
									.map(ForumMapper::fromDTO)
									.toList());
						}
						else if (idName.equals(AddForumMessages.class.getSimpleName()))
						{
							var action = objectMapper.convertValue(sse.data().action(), AddForumMessages.class);

							addForumMessages(action.forumMessages().stream()
									.map(ForumMapper::fromDTO)
									.toList());
						}
						else if (idName.equals(MarkForumMessagesAsRead.class.getSimpleName()))
						{
							var action = objectMapper.convertValue(sse.data().action(), MarkForumMessagesAsRead.class);

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

	private void getForumGroups()
	{
		forumClient.getForumGroups().collectList()
				.doOnSuccess(this::addForumGroups)
				.subscribe();
	}

	private void addForumGroups(List<ForumGroup> forumGroups)
	{
		forumGroups.forEach(forumGroup -> {
			if (!forumGroup.isExternal())
			{
				addOrUpdate(ownForums, forumGroup);
			}
			else if (forumGroup.isSubscribed())
			{
				addOrUpdate(subscribedForums, forumGroup);
			}
			else
			{
				addOrUpdate(popularForums, forumGroup);
			}
		});
		updateForumGroupsUnreadCount(forumGroups);
	}

	private void updateForumGroupsUnreadCount(List<ForumGroup> forumGroups)
	{
		forumGroups.forEach(forumGroup -> forumClient.getForumUnreadCount(forumGroup.getId())
				.doOnSuccess(unreadCount -> Platform.runLater(() -> getSubscribedTreeItemByGxsId(forumGroup.getGxsId())
						.ifPresent(forumGroupTreeItem -> forumGroupTreeItem.getValue().setUnreadCount(unreadCount))))
				.doFinally(_ -> Platform.runLater(this::refreshForumTree))
				.subscribe());
	}

	private void addOrUpdate(TreeItem<ForumGroup> parent, ForumGroup forumGroup)
	{
		var tree = parent.getChildren();

		if (tree.stream()
				.map(TreeItem::getValue)
				.noneMatch(existingForum -> existingForum.equals(forumGroup)))
		{
			tree.add(new TreeItem<>(forumGroup));
			parent.getValue().addUnreadCount(1);
			sortByName(tree);
			removeFromOthers(parent, forumGroup);
		}
	}

	private void removeFromOthers(TreeItem<ForumGroup> parent, ForumGroup forumGroup)
	{
		var removalList = new ArrayList<>(List.of(ownForums, subscribedForums, popularForums, otherForums));
		removalList.remove(parent);

		removalList.forEach(treeItems -> treeItems.getChildren().stream()
				.filter(forumHolderTreeItem -> forumHolderTreeItem.getValue().equals(forumGroup))
				.findFirst()
				.ifPresent(forumGroupTreeItem -> {
					treeItems.getChildren().remove(forumGroupTreeItem);
					treeItems.getValue().subtractUnreadCount(1);
				}));
	}

	private static void sortByName(ObservableList<TreeItem<ForumGroup>> children)
	{
		children.sort((o1, o2) -> o1.getValue().getName().compareToIgnoreCase(o2.getValue().getName()));
	}

	private void subscribeToForumGroup(ForumGroup forumGroup)
	{
		var alreadySubscribed = subscribedForums.getChildren().stream()
				.anyMatch(forumHolderTreeItem -> forumHolderTreeItem.getValue().equals(forumGroup));

		if (!alreadySubscribed)
		{
			forumClient.subscribeToForumGroup(forumGroup.getId())
					.doOnSuccess(_ -> {
						forumGroup.setSubscribed(true);
						addOrUpdate(subscribedForums, forumGroup);
					})
					.subscribe();
		}
	}

	private void unsubscribeFromForumGroups(ForumGroup forumGroup)
	{
		subscribedForums.getChildren().stream()
				.filter(forumHolderTreeItem -> forumHolderTreeItem.getValue().equals(forumGroup))
				.findAny()
				.ifPresent(_ -> forumClient.unsubscribeFromForumGroup(forumGroup.getId())
						.doOnSuccess(_ -> {
							forumGroup.setSubscribed(false);
							addOrUpdate(popularForums, forumGroup);
						}) // XXX: wrong, could be something else then "otherForums"
						.subscribe());
	}

	private void changeSelectedForumGroup(ForumGroup forumGroup)
	{
		selectedForumGroup = forumGroup;
		selectedForumMessage = null;

		getBrowsableTreeItem(forumGroup.getId()).ifPresentOrElse(_ -> forumClient.getForumMessages(forumGroup.getId()).collectList()
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
				.subscribe(), () -> Platform.runLater(() -> {
			// XXX: this is the case when there's no active forum selected. display some forum/tree group info in the message view
			forumMessagesTreeTableView.getSelectionModel().clearSelection();
			forumMessagesRoot.getChildren().clear();
			clearMessage();
			newThread.setDisable(true);
			forumMessagesState(false);
			messageIdToSelect = null;
		}));
	}

	private void forumMessagesState(boolean loading)
	{
		Platform.runLater(() -> {
			forumMessagesTreeTableView.setVisible(!loading);
			forumMessagesProgress.showProgress(loading);
		});
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

	private Optional<TreeItem<ForumGroup>> getBrowsableTreeItem(long forumId)
	{
		return Stream.concat(subscribedForums.getChildren().stream(), ownForums.getChildren().stream())
				.filter(forumGroupTreeItem -> forumGroupTreeItem.getValue().getId() == forumId)
				.findFirst();
	}

	private void changeSelectedForumMessage(ForumMessage forumMessage)
	{
		selectedForumMessage = forumMessage;

		if (forumMessage != null)
		{
			forumClient.getForumMessage(forumMessage.getId())
					.doOnSuccess(message -> Platform.runLater(() -> {
						var contents = markdownService.parse(message.getContent(), EnumSet.of(ParsingMode.PARAGRAPH), uriService);
						messageContent.getChildren().clear();
						messagePane.setVvalue(messagePane.getVmin());
						messageContent.getChildren().addAll(contents.stream()
								.map(Content::getNode).toList());
						messageAuthor.setText(forumMessage.getAuthorName());
						createAuthorContextMenu(forumMessage.getAuthorName(), forumMessage.getAuthorId());
						messageDate.setText(DATE_TIME_PRECISE_DISPLAY.format(forumMessage.getPublished()));
						messageSubject.setText(forumMessage.getName());
						messageHeader.setVisible(true);
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
		messageHeader.setVisible(false);
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

		forumsToSetCount.forEach((gxsId, unreadCount) -> getSubscribedTreeItemByGxsId(gxsId).ifPresent(forumGroupTreeItem -> addUnreadCount(forumGroupTreeItem, unreadCount)));
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
				selectedForumGroup.subtractUnreadCount(1);
				forumMessagesTreeTableView.refresh();
				refreshForumTree();
				return;
			}
		}

		messageMap.forEach((_, _) -> {
			// XXX: implement... boring. not needed yet because we can't mark several entries at once
		});
	}

	private Optional<TreeItem<ForumGroup>> getSubscribedTreeItemByGxsId(GxsId gxsId)
	{
		return Stream.concat(subscribedForums.getChildren().stream(), ownForums.getChildren().stream())
				.filter(forumGroupTreeItem -> forumGroupTreeItem.getValue().getGxsId().equals(gxsId))
				.findFirst();
	}

	private void addUnreadCount(TreeItem<ForumGroup> forumGroupTreeItem, int unreadCount)
	{
		forumGroupTreeItem.getValue().addUnreadCount(unreadCount);
		refreshForumTree();
	}

	private void refreshForumTree()
	{
		boolean hasUnreadMessages = hasUnreadMessages();
		forumTree.refresh();
		unreadService.sendUnreadEvent(UnreadEvent.Element.FORUM, hasUnreadMessages);
	}

	private boolean hasUnreadMessages()
	{
		return hasUnreadMessagesRecursive(forumTree.getRoot());
	}

	private static boolean hasUnreadMessagesRecursive(TreeItem<ForumGroup> item)
	{
		ForumGroup group = item.getValue();
		if (group != null && group.hasNewMessages())
		{
			return true;
		}
		for (TreeItem<ForumGroup> child : item.getChildren())
		{
			if (hasUnreadMessagesRecursive(child))
			{
				return true;
			}
		}
		return false;
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
}
