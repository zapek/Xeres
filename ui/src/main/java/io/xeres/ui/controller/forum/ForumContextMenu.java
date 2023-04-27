package io.xeres.ui.controller.forum;

import javafx.event.Event;
import javafx.event.EventType;
import javafx.scene.control.TreeItem;

import java.io.Serial;

public class ForumContextMenu extends Event
{
	public static final EventType<ForumContextMenu> ALL = new EventType<>("FORUM_CONTEXT_MENU_ALL");
	public static final EventType<ForumContextMenu> SUBSCRIBE = new EventType<>("FORUM_CONTEXT_MENU_SUBSCRIBE");
	public static final EventType<ForumContextMenu> UNSUBSCRIBE = new EventType<>("FORUM_CONTEXT_MENU_UNSUBSCRIBE");

	@Serial
	private static final long serialVersionUID = -9007879320215259163L;

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
