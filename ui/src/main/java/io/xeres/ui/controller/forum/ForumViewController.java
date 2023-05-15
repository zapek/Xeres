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

import io.xeres.common.id.Id;
import io.xeres.common.message.forum.ForumGroup;
import io.xeres.common.message.forum.ForumMessage;
import io.xeres.ui.client.ForumClient;
import io.xeres.ui.controller.Controller;
import io.xeres.ui.support.markdown.Markdown2Flow;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.text.TextFlow;
import net.rgielen.fxweaver.core.FxmlView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import static javafx.scene.control.TableColumn.SortType.DESCENDING;

@Component
@FxmlView(value = "/view/forum/forumview.fxml")
public class ForumViewController implements Controller
{
	private static final Logger log = LoggerFactory.getLogger(ForumViewController.class);

	@FXML
	private TreeView<ForumGroupHolder> forumTree;

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

	private final ResourceBundle bundle;

	private final ForumClient forumClient;

	private final TreeItem<ForumGroupHolder> ownForums;
	private final TreeItem<ForumGroupHolder> subscribedForums;
	private final TreeItem<ForumGroupHolder> popularForums;
	private final TreeItem<ForumGroupHolder> otherForums;

	public ForumViewController(ForumClient forumClient, ResourceBundle bundle)
	{
		this.forumClient = forumClient;
		this.bundle = bundle;

		ownForums = new TreeItem<>(new ForumGroupHolder(bundle.getString("forum.tree.own")));
		subscribedForums = new TreeItem<>(new ForumGroupHolder(bundle.getString("forum.tree.subscribed")));
		popularForums = new TreeItem<>(new ForumGroupHolder(bundle.getString("forum.tree.popular")));
		otherForums = new TreeItem<>(new ForumGroupHolder(bundle.getString("forum.tree.other")));
	}

	@Override
	public void initialize() throws IOException
	{
		log.debug("Trying to get forums list...");

		var root = new TreeItem<>(new ForumGroupHolder());
		//noinspection unchecked
		root.getChildren().addAll(ownForums, subscribedForums, popularForums, otherForums);
		root.setExpanded(true);
		forumTree.setRoot(root);
		forumTree.setShowRoot(false);
		forumTree.setCellFactory(ForumCell::new);
		forumTree.addEventHandler(ForumContextMenu.SUBSCRIBE, event -> subscribeToForumGroup(event.getTreeItem().getValue().getForum()));
		forumTree.addEventHandler(ForumContextMenu.UNSUBSCRIBE, event -> unsubscribeFromForumGroups(event.getTreeItem().getValue().getForum()));

		// XXX
		forumTree.getSelectionModel().selectedItemProperty()
				.addListener((observable, oldValue, newValue) -> changeSelectedForumGroup(newValue.getValue().getForum()));

		// XXX: add double click

		//forumMessagesTableView.setRowFactory(ForumMessageCell::new); // if we want bold, etc...
		tableSubject.setCellValueFactory(new PropertyValueFactory<>("name"));
		tableAuthor.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getAuthorName() != null ? param.getValue().getAuthorName() : Id.toString(param.getValue().getAuthorId())));
		tableDate.setCellFactory(DateCell::new);
		tableDate.setCellValueFactory(new PropertyValueFactory<>("published"));

		forumMessagesTableView.getSortOrder().add(tableDate);
		tableDate.setSortType(DESCENDING);
		tableDate.setSortable(true);

		forumMessagesTableView.getSelectionModel().selectedItemProperty()
				.addListener((observable, oldValue, newValue) -> changeSelectedForumMessage(newValue));

		getForumGroups();
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

		log.debug("Would add {} forums", forumGroups.size());

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

	private void addOrUpdate(ObservableList<TreeItem<ForumGroupHolder>> tree, ForumGroup forumGroup)
	{
		if (tree.stream()
				.map(TreeItem::getValue)
				.noneMatch(existingForum -> existingForum.getForum().equals(forumGroup)))
		{
			tree.add(new TreeItem<>(new ForumGroupHolder(forumGroup)));
			sortByName(tree);
			removeFromOthers(tree, forumGroup);
		}
	}

	private void removeFromOthers(ObservableList<TreeItem<ForumGroupHolder>> tree, ForumGroup forumGroup)
	{
		var removalList = new ArrayList<>(List.of(ownForums.getChildren(), subscribedForums.getChildren(), popularForums.getChildren(), otherForums.getChildren()));
		removalList.remove(tree);

		removalList.forEach(treeItems -> treeItems.stream()
				.filter(forumHolderTreeItem -> forumHolderTreeItem.getValue().getForum().equals(forumGroup))
				.findFirst()
				.ifPresent(treeItems::remove));
	}

	private static void sortByName(ObservableList<TreeItem<ForumGroupHolder>> children)
	{
		children.sort((o1, o2) -> o1.getValue().getForum().getName().compareToIgnoreCase(o2.getValue().getForum().getName()));
	}

	private void subscribeToForumGroup(ForumGroup forumGroup)
	{
		var alreadySubscribed = subscribedForums.getChildren().stream()
				.anyMatch(forumHolderTreeItem -> forumHolderTreeItem.getValue().getForum().equals(forumGroup));

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
				.filter(forumHolderTreeItem -> forumHolderTreeItem.getValue().getForum().equals(forumGroup))
				.findAny()
				.ifPresent(forumHolderTreeItem -> forumClient.unsubscribeFromForumGroup(forumGroup.getId())
						.doOnSuccess(unused -> addOrUpdate(popularForums.getChildren(), forumGroup)) // XXX: wrong, could be something else then "otherForums"
						.subscribe());
	}

	private void changeSelectedForumGroup(ForumGroup forumGroup)
	{
		forumClient.getForumMessages(forumGroup.getId()).collectList()
				.doOnSuccess(forumMessages -> Platform.runLater(() -> {
					forumMessagesTableView.getItems().clear();
					forumMessagesTableView.getItems().addAll(forumMessages);
					forumMessagesTableView.sort();
					messageContent.getChildren().clear();
				}))
				.doOnError(throwable -> log.error("Error while getting the forum messages: {}", throwable.getMessage(), throwable)) // XXX: cleanup on error?
				.subscribe();

	}

	private void changeSelectedForumMessage(ForumMessage forumMessage)
	{
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
}
