package io.xeres.ui.controller.forum;

import io.xeres.common.i18n.I18nUtils;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeView;

public class ForumCell extends TreeCell<ForumGroupHolder>
{
	private final TreeView<ForumGroupHolder> treeView;

	public ForumCell(TreeView<ForumGroupHolder> treeView)
	{
		super();
		this.treeView = treeView;
		setContextMenu(createContextMenu(this));
	}

	@Override
	protected void updateItem(ForumGroupHolder item, boolean empty)
	{
		super.updateItem(item, empty);
		if (empty)
		{
			setText(null);
		}
		else
		{
			setText(item.getForum().getName());
		}
	}

	private ContextMenu createContextMenu(TreeCell<ForumGroupHolder> cell)
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
