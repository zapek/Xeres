package io.xeres.ui.controller.forum;

import io.xeres.common.i18n.I18nUtils;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeView;

public class ForumCell extends TreeCell<ForumHolder>
{
	private final TreeView<ForumHolder> treeView;

	public ForumCell(TreeView<ForumHolder> treeView)
	{
		super();
		this.treeView = treeView;
		setContextMenu(createContextMenu(this));
	}

	private ContextMenu createContextMenu(TreeCell<ForumHolder> cell)
	{
		var contextMenu = new ContextMenu();

		var subscribeItem = new MenuItem(I18nUtils.getString("forum.tree.subscribe"));
		subscribeItem.setOnAction(event -> treeView.fireEvent(new ForumContextMenu(ForumContextMenu.SUBSCRIBE, cell.getTreeItem())));

		var unsubscribeItem = new MenuItem(I18nUtils.getString("forum.tree.unsubscribe"));
		unsubscribeItem.setOnAction(event -> treeView.fireEvent(new ForumContextMenu(ForumContextMenu.UNSUBSCRIBE, cell.getTreeItem())));

		contextMenu.getItems().addAll(subscribeItem, unsubscribeItem);
		return contextMenu;
	}
}
