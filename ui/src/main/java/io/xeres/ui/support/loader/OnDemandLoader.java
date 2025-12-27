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

package io.xeres.ui.support.loader;

import io.xeres.ui.client.GxsMessageClient;
import io.xeres.ui.controller.common.GxsGroup;
import io.xeres.ui.support.util.UiUtils;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.ScrollBar;
import javafx.scene.input.MouseEvent;
import org.fxmisc.flowless.VirtualFlow;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.Queue;

public class OnDemandLoader<G extends GxsGroup, M>
{
	private static final Logger log = LoggerFactory.getLogger(OnDemandLoader.class);

	private static final int PAGE_SIZE = 20;

	/// How close to a border to start prefetching.
	private static final int BORDER_PREFETCH = 0; // XXX: untested...

	private G selectedGroup;

	private int basePage; // Which page is the base page, that is, offset 0 is that page

	// XXX: use those 2 to know if we can insert new data from notifications! actually that won't work... we need to know the first and last date instead :/
	private int lastPage;

	private boolean locked;

	private final Queue<FetchRequest> requests = new LinkedList<>();

	private final VirtualizedScrollPane<VirtualFlow<M, ?>> virtualizedScrollPane;
	private final ObservableList<M> messages;
	private final GxsMessageClient<M> messageClient;

	public OnDemandLoader(VirtualizedScrollPane<?> virtualizedScrollPane, ObservableList<M> messages, GxsMessageClient<M> messageClient)
	{
		//noinspection ConstantValue
		if (BORDER_PREFETCH >= PAGE_SIZE)
		{
			throw new IllegalArgumentException("BORDER_PREFETCH must not be bigger than PAGE_SIZE");
		}

		//noinspection unchecked
		this.virtualizedScrollPane = (VirtualizedScrollPane<VirtualFlow<M, ?>>) virtualizedScrollPane;
		this.messages = messages;
		this.messageClient = messageClient;

		this.virtualizedScrollPane.getContent().needsLayoutProperty().addListener((_, _, newValue) -> {
			if (newValue) // NOSONAR
			{
				doLayout();
			}
		});

		var vbar = getScrollBar();
		if (vbar != null)
		{
			vbar.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
				log.debug("*** MOUSE PRESSED");
				locked = true;
			});

			vbar.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
				log.debug("*** MOUSE RELEASED");
				locked = false;
				doLayout();
			});
		}
	}

	private void doLayout()
	{
		if (locked)
		{
			return;
		}
		var firstVisibleIndex = virtualizedScrollPane.getContent().getFirstVisibleIndex();
		var lastVisibleIndex = virtualizedScrollPane.getContent().getLastVisibleIndex();

		log.debug("layout, first index: {}, last index: {}, total entries: {}", firstVisibleIndex, lastVisibleIndex, messages.size());

		if (firstVisibleIndex == -1 || lastVisibleIndex == -1)
		{
			log.debug("Empty list, doing nothing");
			return;
		}

		if (firstVisibleIndex <= BORDER_PREFETCH)
		{
			log.debug("Calling fetch message BEFORE");
			fetchMessages(FetchMode.BEFORE);
		}
		else if (lastVisibleIndex >= messages.size() - 1 - BORDER_PREFETCH)
		{
			log.debug("Calling fetch message AFTER");
			fetchMessages(FetchMode.AFTER);
		}

		// XXX: add 2 vars (or 1) to track when how close to the top and to the bottom we need to issue a fetch
		// XXX: use a modulo way?
	}

	private ScrollBar getScrollBar()
	{
		Field vbarField;
		try
		{
			vbarField = VirtualizedScrollPane.class.getDeclaredField("vbar");
		}
		catch (NoSuchFieldException _)
		{
			log.error("No such field: vbar");
			return null;
		}
		vbarField.setAccessible(true); // NOSONAR
		try
		{
			return (ScrollBar) vbarField.get(virtualizedScrollPane);
		}
		catch (IllegalAccessException _)
		{
			log.error("No access to vbar");
			return null;
		}
	}

	public void changeSelection(G group)
	{
		selectedGroup = group;
		messages.clear();
		if (selectedGroup != null)
		{
			if (selectedGroup.isSubscribed())
			{
				fetchMessages(FetchMode.ALL);
			}
		}
	}

	private int getPage(int offset)
	{
		return offset / PAGE_SIZE;
	}

	private void fetchMessages(FetchMode fetchMode)
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
				if (basePage + messages.size() / PAGE_SIZE > lastPage) // XXX: double check...
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

					if (paginatedResponse.empty())
					{
						log.warn("Nothing was fetched, ignoring");
						locked = false;
						return;
					}

					lastPage = paginatedResponse.totalPages() - 1; // This keeps the lastPage up to date

					switch (fetchMode)
					{
						case ALL ->
						{
							log.debug("Fetched all ({})", paginatedResponse.numberOfElements());
							messages.addAll(paginatedResponse.content());
							virtualizedScrollPane.getContent().showAsFirst(0);
						}
						case BEFORE ->
						{
							log.debug("Fetching before ({})", paginatedResponse.numberOfElements());
							messages.addAll(0, paginatedResponse.content());
							cleanup(fetchMode);
						}
						case AFTER ->
						{
							log.debug("Fetching after ({})", paginatedResponse.numberOfElements());
							messages.addAll(paginatedResponse.content());
							cleanup(fetchMode);
						}
					}

					locked = false;

					// Request has been processed so remove it
					requests.removeIf(fetchRequest -> fetchRequest.fetchMode() == fetchMode);
				}))
				.doOnError(UiUtils::showAlertError) // XXX: cleanup on error?
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
		while ((messagesToRemove = messages.size() - PAGE_SIZE * 3) > 0)
		{
			log.debug("Trimming message size from {} to {}", messages.size(), PAGE_SIZE * 3);

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
