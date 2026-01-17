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
import javafx.geometry.Orientation;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TreeTableView;
import javafx.scene.input.MouseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class InfiniteTreeListView<M extends GxsMessage> implements InfiniteScrollable
{
	private static final Logger log = LoggerFactory.getLogger(InfiniteTreeListView.class);

	private final TreeTableView<M> treeTableView;
	private final OnDemandLoader<?, M> loader;

	private double scrollBarValue;
	private double scrollBarMin;
	private double scrollBarMax;

	public InfiniteTreeListView(TreeTableView<M> treeTableView, OnDemandLoader<?, M> loader)
	{
		this.loader = loader;
		this.treeTableView = treeTableView;

		this.treeTableView.skinProperty().addListener((_, _, newValue) -> {
			if (newValue != null)
			{
				treeTableView.applyCss();
				treeTableView.layout();

				var found = false;

				for (var node : treeTableView.lookupAll(".scroll-bar"))
				{
					if (node instanceof ScrollBar scrollBar)
					{
						if (scrollBar.getOrientation() == Orientation.VERTICAL)
						{
							setupScrollBarListener(scrollBar);
							found = true;
							break;
						}
					}
				}

				if (!found)
				{
					log.error("Could not find the scroll bar for the InfiniteTreeListView, it won't work properly");
				}
			}
		});
	}

	@Override
	public void scrollToTop()
	{
		treeTableView.scrollTo(0);
	}

	@Override
	public void scrollBackwards(int numberOfEntries)
	{
		treeTableView.scrollTo(numberOfEntries);
	}

	@Override
	public void scrollForwards(int numberOfEntries)
	{
		treeTableView.scrollTo(loader.getTotal() - numberOfEntries);
	}

	private void setupScrollBarListener(ScrollBar scrollBar)
	{
		scrollBar.addEventFilter(MouseEvent.MOUSE_PRESSED, _ -> loader.setLocked(true));

		scrollBar.addEventFilter(MouseEvent.MOUSE_RELEASED, _ -> {
			loader.setLocked(false);
			checkScrolling();
		});

		scrollBar.valueProperty().addListener((observable, oldValue, newValue) -> {
			scrollBarValue = newValue.doubleValue();
			scrollBarMin = scrollBar.getMin();
			scrollBarMax = scrollBar.getMax();
			checkScrolling();
		});
	}

	private void checkScrolling()
	{
		if (loader.isLocked())
		{
			return;
		}
		if (scrollBarValue >= scrollBarMax)
		{
			loader.fetchMessages(FetchMode.AFTER);
		}
		else if (scrollBarValue <= scrollBarMin)
		{
			loader.fetchMessages(FetchMode.BEFORE);
		}
	}
}
