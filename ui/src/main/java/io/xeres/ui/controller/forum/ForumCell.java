package io.xeres.ui.controller.forum;

import io.xeres.common.message.forum.ForumGroup;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeView;

public class ForumCell extends TreeCell<ForumGroup>
{
	public ForumCell(TreeView<ForumGroup> treeView)
	{
		super();
	}

	@Override
	protected void updateItem(ForumGroup item, boolean empty)
	{
		super.updateItem(item, empty);
		if (empty)
		{
			setText(null);
		}
		else
		{
			setText(item.getName());
		}
	}
}
