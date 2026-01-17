/*
 * Copyright (c) 2026 by David Gerber - https://zapek.com
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

import io.xeres.ui.controller.common.GxsMessage;
import javafx.scene.control.ScrollBar;
import javafx.scene.input.MouseEvent;
import org.fxmisc.flowless.VirtualFlow;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

class InfiniteVirtualizedScrollPane<M extends GxsMessage> implements InfiniteScrollable
{
	private static final Logger log = LoggerFactory.getLogger(InfiniteVirtualizedScrollPane.class);

	private final VirtualizedScrollPane<VirtualFlow<M, ?>> virtualizedScrollPane;
	private final OnDemandLoader<?, M> loader;

	public InfiniteVirtualizedScrollPane(VirtualizedScrollPane<?> virtualizedScrollPane, OnDemandLoader<?, M> loader)
	{
		this.loader = loader;
		//noinspection unchecked
		this.virtualizedScrollPane = (VirtualizedScrollPane<VirtualFlow<M, ?>>) virtualizedScrollPane;

		this.virtualizedScrollPane.getContent().needsLayoutProperty().addListener((_, _, newValue) -> {
			if (newValue) // NOSONAR
			{
				checkScrolling();
			}
		});

		var vbar = getVirtualizedScrollPaneScrollBar();
		if (vbar != null)
		{
			vbar.addEventFilter(MouseEvent.MOUSE_PRESSED, _ -> loader.setLocked(true));

			vbar.addEventFilter(MouseEvent.MOUSE_RELEASED, _ -> {
				loader.setLocked(false);
				checkScrolling();
			});
		}
	}

	@Override
	public void scrollToTop()
	{
		virtualizedScrollPane.getContent().showAsFirst(0);
	}

	@Override
	public void scrollBackwards(int numberOfEntries)
	{

	}

	@Override
	public void scrollForwards(int numberOfEntries)
	{

	}

	private ScrollBar getVirtualizedScrollPaneScrollBar()
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

	private void checkScrolling()
	{
		if (loader.isLocked())
		{
			return;
		}
		var firstVisibleIndex = virtualizedScrollPane.getContent().getFirstVisibleIndex();
		var lastVisibleIndex = virtualizedScrollPane.getContent().getLastVisibleIndex();

		log.debug("layout, first index: {}, last index: {}, total entries: {}", firstVisibleIndex, lastVisibleIndex, loader.getTotal());

		if (firstVisibleIndex == -1 || lastVisibleIndex == -1)
		{
			log.debug("Empty list, doing nothing");
			return;
		}

		if (firstVisibleIndex <= loader.getLowerBound())
		{
			log.debug("Calling fetch message BEFORE");
			loader.fetchMessages(FetchMode.BEFORE);
		}
		else if (lastVisibleIndex >= loader.getHigherBound())
		{
			log.debug("Calling fetch message AFTER");
			loader.fetchMessages(FetchMode.AFTER);
		}
	}
}
