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

package io.xeres.ui.support.loader;

import io.xeres.common.id.GxsId;
import io.xeres.ui.client.GxsMessageClient;
import io.xeres.ui.controller.common.GxsGroup;
import io.xeres.ui.controller.common.GxsMessage;
import io.xeres.ui.support.util.UiUtils;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeTableView;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;

/**
 * A loader that detects when the user has scrolled enough to request loading more data. Can be used to navigate paged
 * data without having additional controls to do so.
 *
 * @param <G> the gxs group
 * @param <M> the gxs message
 */
public class OnDemandLoader<G extends GxsGroup, M extends GxsMessage>
{
	private static final Logger log = LoggerFactory.getLogger(OnDemandLoader.class);

	/**
	 * The number of elements requested per page.
	 */
	private static final int PAGE_SIZE = 20;

	/**
	 * The maximum number of pages to keep loaded. When this number is exceeded,
	 * message trimming occurs.
	 */
	private static final int MAXIMUM_PAGES = 3;

	private G selectedGroup;

	private boolean locked;

	private final Queue<FetchRequest> requests = new LinkedList<>();

	private final MessageContainer<M> messageContainer;
	private final GxsMessageClient<M> messageClient;
	private final OnDemandLoaderAction<G> onDemandLoaderAction;

	private InfiniteScrollable infiniteScrollable;

	/**
	 * Creates an OnDemandLoader backed by a VirtualizedScrollPane.
	 * @param virtualizedScrollPane the virtualized scroll pane
	 * @param messages the list of messages
	 * @param messageClient the message client
	 */
	public OnDemandLoader(VirtualizedScrollPane<?> virtualizedScrollPane, ObservableList<M> messages, GxsMessageClient<M> messageClient, OnDemandLoaderAction<G> action)
	{
		this(messages, messageClient, action);
		infiniteScrollable = new InfiniteVirtualizedScrollPane<>(virtualizedScrollPane, this);
	}

	/**
	 * Creates an OnDemandLoader backed by a TreeTableView.
	 * @param treeTableView the tree table view
	 * @param messages the list of messages
	 * @param messageClient the message client
	 */
	public OnDemandLoader(TreeTableView<M> treeTableView, ObservableList<M> messages, GxsMessageClient<M> messageClient, OnDemandLoaderAction<G> action)
	{
		this(messages, messageClient, action);
		infiniteScrollable = new InfiniteTreeListView<>(treeTableView, this);
	}

	private OnDemandLoader(ObservableList<M> messages, GxsMessageClient<M> messageClient, OnDemandLoaderAction<G> action)
	{
		messageContainer = new MessageContainer<>(messages, PAGE_SIZE, MAXIMUM_PAGES);
		this.messageClient = messageClient;
		onDemandLoaderAction = action;
	}

	/**
	 * Changes the selection. This will reset the messages and fetch them for the new group.
	 * @param group the new selection group
	 */
	public void changeSelection(G group)
	{
		selectedGroup = group;
		messageContainer.clear();
		locked = false;
		if (selectedGroup != null)
		{
			if (selectedGroup.isSubscribed())
			{
				fetchMessages(FetchMode.ALL);
				return;
			}
		}
		onDemandLoaderAction.onMessagesLoaded(group);
	}

	/**
	 * Inserts a new message
	 * @param message a new incoming message
	 * @return true if the message has been inserted, false if it has updated an already existing entry
	 */
	public boolean insertMessage(M message)
	{
		if (!isSelectedGroup(message.getGxsId()))
		{
			return false;
		}

		return messageContainer.insert(message);
	}

	/**
	 * Sets the read status of a message.
	 * @param messageId the message id
	 * @param read true if read
	 */
	public void setMessageReadState(long groupId, long messageId, boolean read)
	{
		if (!isSelectedGroup(groupId))
		{
			return;
		}
		messageContainer.setMessageReadState(messageId, read);
	}

	/**
	 * Sets the read count of all messages in a group
	 *
	 * @param groupId the group id
	 * @param read    true if read, false if unread
	 */
	public void setGroupMessagesReadState(long groupId, boolean read)
	{
		if (!isSelectedGroup(groupId))
		{
			log.error("Invalid group id {} when setting read state", groupId);
			return;
		}
		messageContainer.setMessageReadState(null, read);
	}

	private boolean isSelectedGroup(long groupId)
	{
		return selectedGroup != null && selectedGroup.getId() == groupId;
	}

	private boolean isSelectedGroup(GxsId groupGxsId)
	{
		return selectedGroup != null && Objects.equals(selectedGroup.getGxsId(), groupGxsId);
	}

	void setLocked(boolean locked)
	{
		this.locked = locked;
	}

	boolean isLocked()
	{
		return locked;
	}

	int getLowerBound()
	{
		return messageContainer.getLowerBound();
	}

	int getHigherBound()
	{
		return messageContainer.getHigherBound();
	}

	int getTotal()
	{
		return messageContainer.getTotal();
	}

	void fetchMessages(FetchMode fetchMode)
	{
		if (selectedGroup == null || !selectedGroup.isSubscribed())
		{
			log.warn("Attempt to load a group that is not able to");
			return;
		}

		if (requests.stream().anyMatch(fetchRequest -> fetchRequest.fetchMode() == fetchMode))
		{
			log.warn("There's already a pending request with the same fetch mode as {}, ignoring", fetchMode);
			return;
		}

		MessageContainer.MessageClientRequest messageClientRequest = null;

		switch (fetchMode)
		{
			case ALL -> messageClientRequest = messageContainer.prepareFetchAll();
			case BEFORE ->
			{
				if ((messageClientRequest = messageContainer.prepareFetchBefore()) == null)
				{
					log.debug("Already on first page, not fetching anything");
					return;
				}
			}
			case AFTER ->
			{
				if ((messageClientRequest = messageContainer.prepareFetchAfter()) == null)
				{
					log.debug("Already on the last page, not fetching anything");
					return;
				}
			}
		}

		requests.add(new FetchRequest(fetchMode));

		locked = true;

		messageClient.getMessages(selectedGroup.getId(), messageClientRequest.page(), messageClientRequest.size())
				// XXX: progress bar too? only for the first fetch I guess...
				.doOnSuccess(paginatedResponse -> Platform.runLater(() -> {
					assert paginatedResponse != null;

					messageContainer.setTotalPages(paginatedResponse.page().totalPages());

					switch (fetchMode)
					{
						case ALL ->
						{
							log.debug("Fetched all: {}", paginatedResponse);
							if (!paginatedResponse.empty())
							{
								messageContainer.addAfter(paginatedResponse.content());
								infiniteScrollable.scrollToTop();
							}
						}
						case BEFORE ->
						{
							log.debug("Fetched before: {}", paginatedResponse);
							messageContainer.addBefore(paginatedResponse.content());
							infiniteScrollable.scrollBackwards(paginatedResponse.numberOfElements());
						}
						case AFTER ->
						{
							log.debug("Fetched after: {}", paginatedResponse);
							messageContainer.addAfter(paginatedResponse.content());
							infiniteScrollable.scrollForwards(paginatedResponse.numberOfElements());
						}
					}

					locked = false;

					// Request has been processed, so remove it
					requests.removeIf(fetchRequest -> fetchRequest.fetchMode() == fetchMode);
					onDemandLoaderAction.onMessagesLoaded(selectedGroup);
				}))
				.doOnError(UiUtils::webAlertError) // XXX: cleanup on error?
				.subscribe();
	}
}
