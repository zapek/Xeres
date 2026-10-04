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
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

class WindowedMessageContainer<M extends GxsMessage> implements MessageContainer<M>
{
	private static final Logger log = LoggerFactory.getLogger(WindowedMessageContainer.class);

	private final ObservableList<M> messages;
	private final int windowSize;
	private final int concurrentWindows;

	public WindowedMessageContainer(ObservableList<M> messages, int windowSize, int concurrentWindows)
	{
		if (windowSize <= 0)
		{
			throw new IllegalArgumentException("windowSize must be greater than 0");
		}
		if (concurrentWindows <= 0)
		{
			throw new IllegalArgumentException("concurrentWindows must be greater than 0");
		}
		this.messages = messages;
		this.windowSize = windowSize;
		this.concurrentWindows = concurrentWindows;
	}

	@Override
	public void clear()
	{
		messages.clear();
		// XXX
	}

	@Override
	public boolean insert(M message)
	{
		return false;
	}

	@Override
	public void setMessageReadState(Long messageId, boolean read)
	{
		for (var i = 0; i < messages.size(); i++)
		{
			var m = messages.get(i);
			if (messageId == null || m.getId() == messageId)
			{
				if (m.isRead() != read)
				{
					m.setRead(read);
					messages.set(i, m); // This produces flickering (the cell is recreated). Ideally, there should be a way to update cells, see: https://github.com/FXMisc/Flowless/pull/135
				}
				if (messageId != null)
				{
					break;
				}
			}
		}
	}

	@Override
	public void addBefore(List<M> messages)
	{
		this.messages.addAll(0, messages);
		//trim();
	}

	@Override
	public void addAfter(List<M> messages)
	{
		this.messages.addAll(messages);
		//trim();
	}

	@Override
	public int getLowerBound()
	{
		return 0;
	}

	@Override
	public int getHigherBound()
	{
		return messages.size() - 1;
	}

	@Override
	public int getTotal()
	{
		return messages.size();
	}
}
