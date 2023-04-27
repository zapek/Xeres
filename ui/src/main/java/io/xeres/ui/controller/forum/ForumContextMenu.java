package io.xeres.ui.controller.forum;

import javafx.event.Event;
import javafx.event.EventType;
import javafx.scene.control.TreeItem;

public class ForumContextMenu extends Event
{
	public static final EventType<ForumContextMenu> ALL = new EventType<>("FORUM_CONTEXT_MENU_ALL");
	public static final EventType<ForumContextMenu> SUBSCRIBE = new EventType<>("FORUM_CONTEXT_MENU_SUBSCRIBE");
	public static final EventType<ForumContextMenu> UNSUBSCRIBE = new EventType<>("FORUM_CONTEXT_MENU_UNSUBSCRIBE");

	private final transient TreeItem<ForumHolder> treeItem;

	public ForumContextMenu(EventType<ForumContextMenu> eventType, TreeItem<ForumHolder> treeItem)
	{
		super(eventType);
		this.treeItem = treeItem;
	}

	public TreeItem<ForumHolder> getTreeItem()
	{
		return treeItem;
	}
}
