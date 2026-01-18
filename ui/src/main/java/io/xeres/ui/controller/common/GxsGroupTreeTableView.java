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

package io.xeres.ui.controller.common;

import io.xeres.common.i18n.I18nUtils;
import io.xeres.common.id.GxsId;
import io.xeres.ui.client.GxsGroupClient;
import io.xeres.ui.support.contextmenu.XContextMenu;
import io.xeres.ui.support.preference.PreferenceUtils;
import io.xeres.ui.support.util.UiUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignL;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class GxsGroupTreeTableView<T extends GxsGroup> extends TreeTableView<T>
{
	private static final String SUBSCRIBE_MENU_ID = "subscribe";
	private static final String UNSUBSCRIBE_MENU_ID = "unsubscribe";
	private static final String COPY_LINK_MENU_ID = "copyLink";
	private static final String EDIT_MENU_ID = "edit";

	private static final String OPEN_OWN = "OpenOwn";
	private static final String OPEN_SUBSCRIBED = "OpenSubscribed";
	private static final String OPEN_POPULAR = "OpenPopular";
	private static final String OPEN_OTHER = "OpenOther";

	@FXML
	private TreeTableColumn<T, T> groupNameColumn;

	@FXML
	private TreeTableColumn<T, Integer> groupCountColumn;

	private GxsGroupTreeTableAction<T> action;
	private Consumer<Boolean> unreadCountUpdater;

	private TreeItem<T> ownGroups;
	private TreeItem<T> subscribedGroups;
	private TreeItem<T> popularGroups;
	private TreeItem<T> otherGroups;

	private static final ResourceBundle bundle = I18nUtils.getBundle();

	private GxsGroupClient<T> groupClient;

	private T selectedGroup;

	public GxsGroupTreeTableView()
	{
		var loader = new FXMLLoader(GxsGroupTreeTableView.class.getResource("/view/custom/gxs_group_tree_table_view.fxml"), bundle);
		loader.setRoot(this);
		loader.setController(this);

		try
		{
			loader.load();
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	public T getSelectedGroup()
	{
		return selectedGroup;
	}

	public Stream<TreeItem<T>> getAllGroups()
	{
		return Stream.of(ownGroups.getChildren().stream(), // Concat all streams
				subscribedGroups.getChildren().stream(),
				popularGroups.getChildren().stream(),
				otherGroups.getChildren().stream()
		).reduce(Stream.empty(), Stream::concat);
	}

	public Stream<TreeItem<T>> getSubscribedGroups()
	{
		return Stream.concat(subscribedGroups.getChildren().stream(), ownGroups.getChildren().stream());
	}

	public void subtractUnreadCountFromSelected(int unreadCount)
	{
		getSelectedGroup().subtractUnreadCount(unreadCount); // XXX: could this fail? null check?
		refreshTree();
	}

	public void addUnreadCount(Map<GxsId, Integer> groups)
	{
		groups.forEach((gxsId, unreadCount) -> getSubscribedTreeItemByGxsId(gxsId).ifPresent(groupTreeItem -> groupTreeItem.getValue().addUnreadCount(unreadCount)));
		refreshTree();
	}

	public void addGroups(List<T> groups)
	{
		groups.forEach(group -> {
			if (!group.isExternal())
			{
				addOrUpdate(ownGroups, group);
			}
			else if (group.isSubscribed())
			{
				addOrUpdate(subscribedGroups, group);
			}
			else
			{
				addOrUpdate(popularGroups, group);
			}
		});
		updateGroupsUnreadCount(groups);
	}

	public void addOrUpdate(TreeItem<T> parent, T group)
	{
		var tree = parent.getChildren();

		tree.stream()
				.filter(existingTree -> existingTree.getValue().getId() == group.getId())
				.findAny().ifPresentOrElse(found -> {
					found.setValue(group); // XXX: doesn't refresh the image... why? because of the cache? (url is the same after all...)
				}, () -> {
					tree.add(new TreeItem<>(group));
					parent.getValue().addUnreadCount(1);
					sortByName(tree);
					removeFromOthers(parent, group);
				});
	}

	private void subscribeToGroup(T group)
	{
		var alreadySubscribed = subscribedGroups.getChildren().stream()
				.anyMatch(holderTreeItem -> holderTreeItem.getValue().getId() == group.getId());

		if (!alreadySubscribed)
		{
			groupClient.subscribeToGroup(group.getId())
					.doOnSuccess(_ -> {
						group.setSubscribed(true);
						addOrUpdate(subscribedGroups, group);
					})
					.subscribe();
		}
	}

	private void unsubscribeFromGroup(T group)
	{
		subscribedGroups.getChildren().stream()
				.filter(holderTreeItem -> holderTreeItem.getValue().getId() == group.getId())
				.findAny()
				.ifPresent(_ -> groupClient.unsubscribeFromGroup(group.getId())
						.doOnSuccess(_ -> {
							group.setSubscribed(false);
							addOrUpdate(popularGroups, group);
						}) // XXX: wrong, could be something else then "others"
						.subscribe());
	}

	private void updateGroupsUnreadCount(List<T> groups)
	{
		groups.forEach(group -> groupClient.getUnreadCount(group.getId())
				.doOnSuccess(unreadCount -> Platform.runLater(() -> getSubscribedTreeItemByGxsId(group.getGxsId())
						.ifPresent(groupTreeItem -> groupTreeItem.getValue().setUnreadCount(unreadCount))))
				.doFinally(_ -> Platform.runLater(this::refreshTree))
				.subscribe());
	}

	private Optional<TreeItem<T>> getSubscribedTreeItemByGxsId(GxsId gxsId)
	{
		return Stream.concat(subscribedGroups.getChildren().stream(), ownGroups.getChildren().stream())
				.filter(groupTreeItem -> groupTreeItem.getValue().getGxsId().equals(gxsId))
				.findFirst();
	}

	private void refreshTree()
	{
		boolean hasUnreadMessages = hasUnreadMessages();
		refresh();
		unreadCountUpdater.accept(hasUnreadMessages);
	}

	private boolean hasUnreadMessages()
	{
		return hasUnreadMessagesRecursive(getRoot());
	}

	private boolean hasUnreadMessagesRecursive(TreeItem<T> item)
	{
		var group = item.getValue();
		if (group != null && group.hasNewMessages())
		{
			return true;
		}
		for (TreeItem<T> child : item.getChildren())
		{
			if (hasUnreadMessagesRecursive(child))
			{
				return true;
			}
		}
		return false;
	}

	private void sortByName(ObservableList<TreeItem<T>> children)
	{
		children.sort((o1, o2) -> o1.getValue().getName().compareToIgnoreCase(o2.getValue().getName()));
	}

	private void removeFromOthers(TreeItem<T> parent, T group)
	{
		var removalList = new ArrayList<>(List.of(ownGroups, subscribedGroups, popularGroups, otherGroups));
		removalList.remove(parent);

		removalList.forEach(treeItems -> treeItems.getChildren().stream()
				.filter(boardHolderTreeItem -> boardHolderTreeItem.getValue().getId() == group.getId())
				.findFirst()
				.ifPresent(boardGroupTreeItem -> {
					treeItems.getChildren().remove(boardGroupTreeItem);
					treeItems.getValue().subtractUnreadCount(1);
				}));
	}

	public void initialize(String preferenceNodeName, GxsGroupClient<T> groupClient, Function<String, T> groupCreator, Supplier<TreeTableCell<T, T>> cellCreator, GxsGroupTreeTableAction<T> action, Consumer<Boolean> unreadCountUpdater)
	{
		this.action = action;
		this.unreadCountUpdater = unreadCountUpdater;
		this.groupClient = groupClient;

		ownGroups = new TreeItem<>(groupCreator.apply(bundle.getString("gxs-group.tree.own")));
		subscribedGroups = new TreeItem<>(groupCreator.apply(bundle.getString("gxs-group.tree.subscribed")));
		popularGroups = new TreeItem<>(groupCreator.apply(bundle.getString("gxs-group.tree.popular")));
		otherGroups = new TreeItem<>(groupCreator.apply(bundle.getString("gxs-group.tree.other")));

		var root = new TreeItem<>(groupCreator.apply(""));
		//noinspection unchecked
		root.getChildren().addAll(ownGroups, subscribedGroups, popularGroups, otherGroups);
		root.setExpanded(true);
		setRoot(root);
		setShowRoot(false);
		groupNameColumn.setCellFactory(_ -> cellCreator.get());
		groupNameColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getValue()));
		groupCountColumn.setCellFactory(_ -> new GxsGroupCellCount<>());
		groupCountColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("unreadCount"));
		createTreeContextMenu();

		// We need Platform.runLater() because when an entry is moved, the selection can change
		getSelectionModel().selectedItemProperty()
				.addListener((_, _, newValue) -> Platform.runLater(() -> {
					selectedGroup = newValue != null ? newValue.getValue() : null;

					if (selectedGroup == null)
					{
						action.onUnselect();
					}
					else
					{
						getSubscribedGroups()
								.filter(forumGroupTreeItem -> forumGroupTreeItem.getValue().getId() == selectedGroup.getId())
								.findFirst()
								.ifPresentOrElse(_ -> action.onSelectSubscribed(selectedGroup),
										() -> action.onSelectUnsubscribed(selectedGroup));
					}
				}));

		UiUtils.setOnPrimaryMouseDoubleClicked(this, _ -> {
			if (isGroupSelected())
			{
				subscribeToGroup(selectedGroup);
			}
		});

		setupTrees(preferenceNodeName);

		getGroups();
	}

	private void setupTrees(String nodeName)
	{
		var node = PreferenceUtils.getPreferences().node(nodeName);
		ownGroups.setExpanded(node.getBoolean(OPEN_OWN, false));
		subscribedGroups.setExpanded(node.getBoolean(OPEN_SUBSCRIBED, false));
		popularGroups.setExpanded(node.getBoolean(OPEN_POPULAR, false));
		otherGroups.setExpanded(node.getBoolean(OPEN_OTHER, false));

		ownGroups.expandedProperty().addListener((_, _, newValue) -> node.putBoolean(OPEN_OWN, newValue));
		subscribedGroups.expandedProperty().addListener((_, _, newValue) -> node.putBoolean(OPEN_SUBSCRIBED, newValue));
		popularGroups.expandedProperty().addListener((_, _, newValue) -> node.putBoolean(OPEN_POPULAR, newValue));
		otherGroups.expandedProperty().addListener((_, _, newValue) -> node.putBoolean(OPEN_OTHER, newValue));
	}

	private void getGroups()
	{
		groupClient.getGroups().collectList()
				.doOnSuccess(this::addGroups)
				.subscribe();
	}

	private boolean isGroupSelected()
	{
		return selectedGroup != null && selectedGroup.isReal();
	}

	private void createTreeContextMenu()
	{
		var editItem = new MenuItem("Edit");
		editItem.setId(EDIT_MENU_ID);
		// XXX: find fonticon
		editItem.setOnAction(event -> {
			//noinspection unchecked
			var group = ((TreeItem<T>) event.getSource()).getValue();
			action.onEdit(group);
		});

		var subscribeItem = new MenuItem(bundle.getString("gxs-group.tree.subscribe"));
		subscribeItem.setId(SUBSCRIBE_MENU_ID);
		subscribeItem.setGraphic(new FontIcon(MaterialDesignL.LOCATION_ENTER));
		subscribeItem.setOnAction(event -> {
			//noinspection unchecked
			var group = ((TreeItem<T>) event.getSource()).getValue();
			subscribeToGroup(group);
			action.onSubscribe(group);
		});

		var unsubscribeItem = new MenuItem(bundle.getString("gxs-group.tree.unsubscribe"));
		unsubscribeItem.setId(UNSUBSCRIBE_MENU_ID);
		unsubscribeItem.setGraphic(new FontIcon(MaterialDesignL.LOCATION_EXIT));
		unsubscribeItem.setOnAction(event -> {
			//noinspection unchecked
			var group = ((TreeItem<T>) event.getSource()).getValue();
			unsubscribeFromGroup(group);
			action.onUnsubscribe(group);
		});

		var copyLinkItem = new MenuItem(bundle.getString("copy-link"));
		copyLinkItem.setId(COPY_LINK_MENU_ID);
		copyLinkItem.setGraphic(new FontIcon(MaterialDesignL.LINK_VARIANT));
		//noinspection unchecked
		copyLinkItem.setOnAction(event -> action.onCopyLink(((TreeItem<T>) event.getSource()).getValue()));

		var xContextMenu = new XContextMenu<TreeItem<T>>(subscribeItem, unsubscribeItem, editItem, new SeparatorMenuItem(), copyLinkItem);
		xContextMenu.addToNode(this);
		xContextMenu.setOnShowing((contextMenu, treeItem) -> {
			if (treeItem == null)
			{
				return false;
			}

			if (!treeItem.getValue().isReal())
			{
				return false;
			}

			if (treeItem.getValue().isExternal())
			{
				contextMenu.getItems().stream()
						.filter(menuItem -> SUBSCRIBE_MENU_ID.equals(menuItem.getId()))
						.findFirst().ifPresent(menuItem -> menuItem.setDisable(treeItem.getValue().isSubscribed()));

				contextMenu.getItems().stream()
						.filter(menuItem -> UNSUBSCRIBE_MENU_ID.equals(menuItem.getId()))
						.findFirst().ifPresent(menuItem -> menuItem.setDisable(!treeItem.getValue().isSubscribed()));

				contextMenu.getItems().stream()
						.filter(menuItem -> EDIT_MENU_ID.equals(menuItem.getId()))
						.findFirst().ifPresent(menuItem -> menuItem.setVisible(false));
				return true;
			}
			else
			{
				contextMenu.getItems().stream()
						.filter(menuItem -> SUBSCRIBE_MENU_ID.equals(menuItem.getId()))
						.findFirst().ifPresent(menuItem -> menuItem.setVisible(false));

				contextMenu.getItems().stream()
						.filter(menuItem -> UNSUBSCRIBE_MENU_ID.equals(menuItem.getId()))
						.findFirst().ifPresent(menuItem -> menuItem.setVisible(false));
				return true;
			}
		});
	}

}
