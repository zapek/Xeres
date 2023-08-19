/*
 * Copyright (c) 2023 by David Gerber - https://zapek.com
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
import io.xeres.common.i18n.I18nUtils;
import io.xeres.common.id.Id;
import io.xeres.common.message.forum.ForumGroup;
import io.xeres.common.message.forum.ForumMessage;
import io.xeres.common.rest.forum.PostRequest;
import io.xeres.common.rest.notification.forum.AddForumGroups;
import io.xeres.common.rest.notification.forum.AddForumMessages;
import io.xeres.ui.client.ForumClient;
import io.xeres.ui.client.NotificationClient;
import io.xeres.ui.controller.Controller;
import io.xeres.ui.model.forum.ForumMapper;
import io.xeres.ui.support.contextmenu.XContextMenu;
import io.xeres.ui.support.markdown.Markdown2Flow;
import io.xeres.ui.support.util.UiUtils;
import io.xeres.ui.support.window.WindowManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.text.TextFlow;
import javafx.stage.Window;
import net.rgielen.fxweaver.core.FxmlView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;

import java.io.IOException;
import java.time.Instant;
import java.util.*;

import static javafx.scene.control.TableColumn.SortType.DESCENDING;

@Component
@FxmlView(value = "/view/forum/forumview.fxml")
public class ForumViewController implements Controller
{
	private static final Logger log = LoggerFactory.getLogger(ForumViewController.class);
	public static final String SUBSCRIBE_MENU_ID = "subscribe";
	public static final String UNSUBSCRIBE_MENU_ID = "unsubscribe";

	@FXML
	private TreeView<ForumGroup> forumTree;

	@FXML
	private SplitPane splitPaneVertical;

	@FXML
	private SplitPane splitPaneHorizontal;

	@FXML
	private TableView<ForumMessage> forumMessagesTableView;

	@FXML
	private TableColumn<ForumMessage, String> tableSubject;

	@FXML
	private TableColumn<ForumMessage, String> tableAuthor;

	@FXML
	private TableColumn<ForumMessage, Instant> tableDate;

	@FXML
	private TextFlow messageContent;

	@FXML
	public Button createForum;

	@FXML
	private Button newThread;

	private final ResourceBundle bundle;

	private final ForumClient forumClient;
	private final NotificationClient notificationClient;
	private final WindowManager windowManager;
	private final ObjectMapper objectMapper;
	private ForumGroup selectedForumGroup;
	private ForumMessage selectedForumMessage;

	private Disposable notificationDisposable;

	private XContextMenu<ForumGroup> forumGroupXContextMenu;
	private XContextMenu<ForumMessage> forumMessageXContextMenu;

	private final TreeItem<ForumGroup> ownForums;
	private final TreeItem<ForumGroup> subscribedForums;
	private final TreeItem<ForumGroup> popularForums;
	private final TreeItem<ForumGroup> otherForums;

	public ForumViewController(ForumClient forumClient, ResourceBundle bundle, NotificationClient notificationClient, WindowManager windowManager, ObjectMapper objectMapper)
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
	}

	@Override
	public void initialize() throws IOException
	{
		log.debug("Trying to get forums list...");

		var root = new TreeItem<>(new ForumGroup(""));
		//noinspection unchecked
		root.getChildren().addAll(ownForums, subscribedForums, popularForums, otherForums);
		root.setExpanded(true);
		forumTree.setRoot(root);
		forumTree.setShowRoot(false);
		forumTree.setCellFactory(param -> new ForumCell());
		createForumTreeContextMenu();

		// We need Platform.runLater() because when an entry is moved, the selection can change
		forumTree.getSelectionModel().selectedItemProperty()
				.addListener((observable, oldValue, newValue) -> Platform.runLater(() -> changeSelectedForumGroup(newValue.getValue())));

		// XXX: add double click
		forumTree.setOnMouseClicked(event -> {
			if (event.getClickCount() == 2 && isForumSelected())
			{
				subscribeToForumGroup(selectedForumGroup);
			}
		});

		forumMessagesTableView.setRowFactory(param -> new ForumMessageCell());
		createForumMessageTableViewContextMenu();
		tableSubject.setCellValueFactory(new PropertyValueFactory<>("name"));
		tableAuthor.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getAuthorName() != null ? param.getValue().getAuthorName() : Id.toString(param.getValue().getAuthorId())));
		tableDate.setCellFactory(param -> new DateCell());
		tableDate.setCellValueFactory(new PropertyValueFactory<>("published"));

		forumMessagesTableView.getSortOrder().add(tableDate);
		tableDate.setSortType(DESCENDING);
		tableDate.setSortable(true);

		forumMessagesTableView.getSelectionModel().selectedItemProperty()
				.addListener((observable, oldValue, newValue) -> changeSelectedForumMessage(newValue));

		createForum.setOnAction(event -> windowManager.openForumCreation(UiUtils.getWindow(event)));

		newThread.setOnAction(event -> newForumPost(UiUtils.getWindow(event), false));

		setupForumNotifications();

		getForumGroups();
	}

	private void createForumTreeContextMenu()
	{
		var subscribeItem = new MenuItem(I18nUtils.getString("forum.tree.subscribe"));
		subscribeItem.setId(SUBSCRIBE_MENU_ID);
		subscribeItem.setOnAction(event -> subscribeToForumGroup((ForumGroup) event.getSource()));

		var unsubscribeItem = new MenuItem(I18nUtils.getString("forum.tree.unsubscribe"));
		unsubscribeItem.setId(UNSUBSCRIBE_MENU_ID);
		unsubscribeItem.setOnAction(event -> unsubscribeFromForumGroups((ForumGroup) event.getSource()));

		forumGroupXContextMenu = new XContextMenu<>(forumTree, subscribeItem, unsubscribeItem);
		forumGroupXContextMenu.setOnShowing((contextMenu, forumGroup) -> {
			contextMenu.getItems().stream()
					.filter(menuItem -> menuItem.getId().equals(SUBSCRIBE_MENU_ID))
					.findFirst().ifPresent(menuItem -> menuItem.setDisable(forumGroup.isSubscribed()));

			contextMenu.getItems().stream()
					.filter(menuItem -> menuItem.getId().equals(UNSUBSCRIBE_MENU_ID))
					.findFirst().ifPresent(menuItem -> menuItem.setDisable(!forumGroup.isSubscribed()));

			return forumGroup.isReal();
		});
	}

	private void createForumMessageTableViewContextMenu()
	{
		var replyItem = new MenuItem("Reply");
		replyItem.setOnAction(event -> newForumPost(UiUtils.getWindow(event), true));

		forumMessageXContextMenu = new XContextMenu<>(forumMessagesTableView, replyItem);
	}

	private void newForumPost(Window window, boolean replyTo)
	{
		var replyToId = 0L;
		var originalId = 0L;

		if (selectedForumMessage != null)
		{
			replyToId = replyTo ? selectedForumMessage.getId() : 0L;
			//originalId = selectedForumMessage.getOriginalId(); // XXX: means edit. if desired, set it to its current id
		}

		var postRequest = new PostRequest(selectedForumGroup.getId(), originalId, replyToId);
		windowManager.openForumEditor(window, postRequest);
	}

	private boolean isForumSelected()
	{
		return selectedForumGroup != null && selectedForumGroup.isReal();
	}

	private void setupForumNotifications()
	{
		notificationDisposable = notificationClient.getForumNotifications()
				.doOnComplete(() -> log.debug("Notification connection closed"))
				.doOnError(throwable -> log.debug("Notification error: {}", throwable.getMessage()))
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
						else
						{
							log.debug("Unknown forum notification");
						}
						// XXX: add message, etc... but only if the group is already selected
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
		var subscribedTree = subscribedForums.getChildren();
		var popularTree = popularForums.getChildren();
		var otherTree = otherForums.getChildren();

		forumGroups.forEach(forumGroup -> {
			if (forumGroup.isSubscribed())
			{
				addOrUpdate(subscribedTree, forumGroup);
			}
			else
			{
				addOrUpdate(popularTree, forumGroup);
			}
		});
	}

	private void addOrUpdate(ObservableList<TreeItem<ForumGroup>> tree, ForumGroup forumGroup)
	{
		if (tree.stream()
				.map(TreeItem::getValue)
				.noneMatch(existingForum -> existingForum.equals(forumGroup)))
		{
			tree.add(new TreeItem<>(forumGroup));
			sortByName(tree);
			removeFromOthers(tree, forumGroup);
		}
	}

	private void removeFromOthers(ObservableList<TreeItem<ForumGroup>> tree, ForumGroup forumGroup)
	{
		var removalList = new ArrayList<>(List.of(ownForums.getChildren(), subscribedForums.getChildren(), popularForums.getChildren(), otherForums.getChildren()));
		removalList.remove(tree);

		removalList.forEach(treeItems -> treeItems.stream()
				.filter(forumHolderTreeItem -> forumHolderTreeItem.getValue().equals(forumGroup))
				.findFirst()
				.ifPresent(treeItems::remove));
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
					.doOnSuccess(forumId -> addOrUpdate(subscribedForums.getChildren(), forumGroup))
					.subscribe();
		}
	}

	private void unsubscribeFromForumGroups(ForumGroup forumGroup)
	{
		subscribedForums.getChildren().stream()
				.filter(forumHolderTreeItem -> forumHolderTreeItem.getValue().equals(forumGroup))
				.findAny()
				.ifPresent(forumHolderTreeItem -> forumClient.unsubscribeFromForumGroup(forumGroup.getId())
						.doOnSuccess(unused -> addOrUpdate(popularForums.getChildren(), forumGroup)) // XXX: wrong, could be something else then "otherForums"
						.subscribe());
	}

	private void changeSelectedForumGroup(ForumGroup forumGroup)
	{
		selectedForumGroup = forumGroup;
		selectedForumMessage = null;

		getSubscribedTreeItem(forumGroup.getId()).ifPresentOrElse(forumGroupTreeItem -> forumClient.getForumMessages(forumGroup.getId()).collectList()
				.doOnSuccess(forumMessages -> Platform.runLater(() -> {
					forumMessagesTableView.getItems().clear();
					forumMessagesTableView.getItems().addAll(forumMessages);
					forumMessagesTableView.sort();
					messageContent.getChildren().clear();
					newThread.setDisable(false);
				}))
				.doOnError(throwable -> log.error("Error while getting the forum messages: {}", throwable.getMessage(), throwable)) // XXX: cleanup on error?
				.subscribe(), () -> {
			// XXX: display some forum info in the message view
			forumMessagesTableView.getItems().clear();
			messageContent.getChildren().clear();
			newThread.setDisable(true);
		});
	}

	private Optional<TreeItem<ForumGroup>> getSubscribedTreeItem(long forumId)
	{
		return subscribedForums.getChildren().stream()
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
						var md2flow = new Markdown2Flow(message.getContent());

						messageContent.getChildren().clear();
						messageContent.getChildren().addAll(md2flow.getNodes());
					}))
					.subscribe();
		}
	}

	private void addForumMessages(List<ForumMessage> forumMessages)
	{
		if (selectedForumGroup == null)
		{
			return;
		}

		forumMessages.forEach(forumMessage -> {
			if (forumMessage.getGxsId().equals(selectedForumGroup.getGxsId()))
			{
				add(forumMessage);
			}
		});
	}

	private void add(ForumMessage forumMessage)
	{
		forumMessagesTableView.getItems().add(forumMessage);
		forumMessagesTableView.sort();
	}

	@EventListener
	public void onApplicationEvent(ContextClosedEvent ignored)
	{
		if (notificationDisposable != null && !notificationDisposable.isDisposed())
		{
			notificationDisposable.dispose();
		}
	}
}
