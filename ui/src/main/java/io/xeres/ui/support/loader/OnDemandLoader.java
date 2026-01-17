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
import java.util.Queue;

public class OnDemandLoader<G extends GxsGroup, M extends GxsMessage>
{
	private static final Logger log = LoggerFactory.getLogger(OnDemandLoader.class);

	private static final int PAGE_SIZE = 20;

	private static final int MAXIMUM_PAGES = 3;

	/// How close to a border to start prefetching.
	private static final int BORDER_PREFETCH = 0; // XXX: untested...

	private G selectedGroup;

	private int basePage; // Which page is the base page, that is, offset 0 is that page

	// XXX: use those 2 to know if we can insert new data from notifications! actually that won't work... we need to know the first and last date instead :/
	private int lastPage;

	private boolean locked;

	private final Queue<FetchRequest> requests = new LinkedList<>();

	private final ObservableList<M> messages;
	private final GxsMessageClient<M> messageClient;

	private final InfiniteScrollable infiniteScrollable;

	public OnDemandLoader(VirtualizedScrollPane<?> virtualizedScrollPane, ObservableList<M> messages, GxsMessageClient<M> messageClient)
	{
		checkConstraints();

		infiniteScrollable = new InfiniteVirtualizedScrollPane<>(virtualizedScrollPane, this);
		this.messages = messages;
		this.messageClient = messageClient;
	}

	public OnDemandLoader(TreeTableView<M> treeTableView, ObservableList<M> messages, GxsMessageClient<M> messageClient)
	{
		checkConstraints();

		infiniteScrollable = new InfiniteTreeListView<>(treeTableView, this);
		this.messages = messages;
		this.messageClient = messageClient;
	}

	void setLocked(boolean locked)
	{
		this.locked = locked;
	}

	boolean isLocked()
	{
		return locked;
	}

	private void checkConstraints()
	{
		//noinspection ConstantValue
		if (BORDER_PREFETCH >= PAGE_SIZE)
		{
			throw new IllegalArgumentException("BORDER_PREFETCH must not be bigger than PAGE_SIZE");
		}
	}

	public void changeSelection(G group)
	{
		selectedGroup = group;
		messages.clear();
		locked = false;
		if (selectedGroup != null)
		{
			if (selectedGroup.isSubscribed())
			{
				fetchMessages(FetchMode.ALL);
			}
		}
	}

	public boolean insertMessage(M message)
	{
		var existingMessage = messages.stream()
				.filter(existing -> existing.getId() == message.getId())
				.findFirst();
		if (existingMessage.isPresent())
		{
			messages.set(messages.indexOf(existingMessage.get()), message);
			return false;
		}
		else
		{
			var size = messages.size();
			if (size == 0)
			{
				messages.add(message);
				return true;
			}

			for (var i = 0; i < size; i++)
			{
				if (message.getPublished().isAfter(messages.get(i).getPublished()))
				{
					messages.add(i, message);
					return true;
				}
			}

			if (size < MAXIMUM_PAGES)
			{
				messages.addLast(message);
				return true;
			}

			// We are after, no need to insert
			return false;
		}
	}

	int getLowerBound()
	{
		return BORDER_PREFETCH;
	}

	int getHigherBound()
	{
		return messages.size() - 1 - BORDER_PREFETCH;
	}

	int getTotal()
	{
		return messages.size();
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

		var page = 0;

		switch (fetchMode)
		{
			case ALL ->
			{
				basePage = 0;
			}
			case BEFORE ->
			{
				if (basePage == 0)
				{
					log.debug("Already on first page, not fetching anything");
					return;
				}
				page = basePage - 1;
			}
			case AFTER ->
			{
				if (messages.size() < PAGE_SIZE || basePage + messages.size() / PAGE_SIZE > lastPage) // XXX: double check... added messages.size() smaller condition...
				{
					log.debug("Already on the last page, not fetching anything");
					return;
				}
				page = basePage + messages.size() / PAGE_SIZE;
			}
		}

		requests.add(new FetchRequest(fetchMode));

		log.debug("Fetching page {}", page);
		locked = true;

		messageClient.getMessages(selectedGroup.getId(), page, PAGE_SIZE)
				// XXX: progress bar too? only for the first fetch I guess...
				.doOnSuccess(paginatedResponse -> Platform.runLater(() -> {
					assert paginatedResponse != null;

					lastPage = paginatedResponse.page().totalPages() - 1; // This keeps the lastPage up to date

					switch (fetchMode)
					{
						case ALL ->
						{
							log.debug("Fetched all ({})", paginatedResponse.numberOfElements());
							if (!paginatedResponse.empty())
							{
								messages.addAll(paginatedResponse.content());
								infiniteScrollable.scrollToTop();
							}
						}
						case BEFORE ->
						{
							log.debug("Fetching before ({})", paginatedResponse.numberOfElements());
							messages.addAll(0, paginatedResponse.content());
							cleanup(fetchMode);
							infiniteScrollable.scrollBackwards(paginatedResponse.numberOfElements());
						}
						case AFTER ->
						{
							log.debug("Fetching after ({})", paginatedResponse.numberOfElements());
							messages.addAll(paginatedResponse.content());
							cleanup(fetchMode);
							infiniteScrollable.scrollForwards(paginatedResponse.numberOfElements());
						}
					}

					locked = false;

					// Request has been processed so remove it
					requests.removeIf(fetchRequest -> fetchRequest.fetchMode() == fetchMode);
				}))
				.doOnError(UiUtils::webAlertError) // XXX: cleanup on error?
				.subscribe();
	}

	private int cleanup(FetchMode fetchMode)
	{
		var totalRemoved = 0;
		if (requests.stream().noneMatch(fetchRequest -> fetchRequest.fetchMode() == fetchMode))
		{
			log.warn("Missing request {} for cleanup action. Shouldn't happen", fetchMode);
			return totalRemoved;
		}

		int messagesToRemove;
		while ((messagesToRemove = messages.size() - PAGE_SIZE * MAXIMUM_PAGES) > 0)
		{
			log.debug("Trimming message size from {} to {}", messages.size(), PAGE_SIZE * MAXIMUM_PAGES);

			var sliceToRemove = Math.min(PAGE_SIZE, messagesToRemove);

			// Find which side to chop off
			switch (fetchMode)
			{
				case AFTER ->
				{
					log.debug("Trimming {} from beginning", sliceToRemove);
					messages.remove(0, sliceToRemove);
					basePage++; // XXX: too simplistic... what happens if sliceToRemove is less than PAGE_SIZE?

					totalRemoved -= sliceToRemove;
				}
				case BEFORE ->
				{
					log.debug("Trimming {} from end", sliceToRemove);
					messages.remove(messages.size() - sliceToRemove, messages.size());
					basePage--; // XXX: ditto...

					totalRemoved -= sliceToRemove;
				}
				case null, default -> log.error("Can't happen");
			}
		}
		return totalRemoved;
	}
}
