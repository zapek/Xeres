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

package io.xeres.ui.controller.common;

import io.xeres.common.i18n.I18nUtils;
import io.xeres.ui.support.contextmenu.XContextMenu;
import io.xeres.ui.support.preference.PreferenceUtils;
import io.xeres.ui.support.util.UiUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignL;

import java.io.IOException;
import java.util.ResourceBundle;
import java.util.function.Function;
import java.util.function.Supplier;

public class GxsGroupTreeTableView<T extends GxsGroupAttribute> extends TreeTableView<T>
{
	private static final String SUBSCRIBE_MENU_ID = "subscribe";
	private static final String UNSUBSCRIBE_MENU_ID = "unsubscribe";
	private static final String COPY_LINK_MENU_ID = "copyLink";

	private static final String OPEN_OWN = "OpenOwn";
	private static final String OPEN_SUBSCRIBED = "OpenSubscribed";
	private static final String OPEN_POPULAR = "OpenPopular";
	private static final String OPEN_OTHER = "OpenOther";

	@FXML
	private TreeTableColumn<T, String> groupNameColumn;

	@FXML
	private TreeTableColumn<T, Integer> groupCountColumn;

	private GxsGroupTreeTableAction<T> action;

	private TreeItem<T> ownGroups;
	private TreeItem<T> subscribedGroups;
	private TreeItem<T> popularGroups;
	private TreeItem<T> otherGroups;

	private static final ResourceBundle bundle = I18nUtils.getBundle();

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

	public TreeItem<T> getOwnGroups()
	{
		return ownGroups;
	}

	public TreeItem<T> getSubscribedGroups()
	{
		return subscribedGroups;
	}

	public TreeItem<T> getPopularGroups()
	{
		return popularGroups;
	}

	public TreeItem<T> getOtherGroups()
	{
		return otherGroups;
	}

	public void initialize(String preferenceNodeName, Function<String, T> groupCreator, Supplier<TreeTableRow<T>> cellCreator, GxsGroupTreeTableAction<T> action)
	{
		this.action = action;

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
		setRowFactory(_ -> cellCreator.get());
		groupNameColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("name"));
		groupCountColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("unreadCount"));
		groupCountColumn.setCellFactory(_ -> new GxsGroupCellCount<>());
		createTreeContextMenu();

		// We need Platform.runLater() because when an entry is moved, the selection can change
		getSelectionModel().selectedItemProperty()
				.addListener((_, _, newValue) -> Platform.runLater(() -> {
					selectedGroup = newValue != null ? newValue.getValue() : null;
					action.onSelect(selectedGroup);
				}));

		UiUtils.setOnPrimaryMouseDoubleClicked(this, _ -> {
			if (isGroupSelected())
			{
				action.onSelect(selectedGroup);
			}
		});

		setupTrees(preferenceNodeName);
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

	private boolean isGroupSelected()
	{
		return selectedGroup != null && selectedGroup.isReal();
	}

	private void createTreeContextMenu()
	{
		var subscribeItem = new MenuItem(bundle.getString("gxs-group.tree.subscribe"));
		subscribeItem.setId(SUBSCRIBE_MENU_ID);
		subscribeItem.setGraphic(new FontIcon(MaterialDesignL.LOCATION_ENTER));
		//noinspection unchecked
		subscribeItem.setOnAction(event -> action.onSubscribe(((TreeItem<T>) event.getSource()).getValue()));

		var unsubscribeItem = new MenuItem(bundle.getString("gxs-group.tree.unsubscribe"));
		unsubscribeItem.setId(UNSUBSCRIBE_MENU_ID);
		unsubscribeItem.setGraphic(new FontIcon(MaterialDesignL.LOCATION_EXIT));
		//noinspection unchecked
		unsubscribeItem.setOnAction(event -> action.onUnsubscribe(((TreeItem<T>) event.getSource()).getValue()));

		var copyLinkItem = new MenuItem(bundle.getString("copy-link"));
		copyLinkItem.setId(COPY_LINK_MENU_ID);
		copyLinkItem.setGraphic(new FontIcon(MaterialDesignL.LINK_VARIANT));
		//noinspection unchecked
		copyLinkItem.setOnAction(event -> action.onCopyLink(((TreeItem<T>) event.getSource()).getValue()));

		var xContextMenu = new XContextMenu<TreeItem<T>>(subscribeItem, unsubscribeItem, new SeparatorMenuItem(), copyLinkItem);
		xContextMenu.addToNode(this);
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

}
