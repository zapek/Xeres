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

/**
 * Class to keep pagination under control, especially when the last data is smaller than a page.
 * This likely doesn't work properly when the underlying data changes. Rewrite everything use a WindowIterator, not pagination.
 *
 * @param <M>
 */
class MessageContainer<M extends GxsMessage>
{
	private static final Logger log = LoggerFactory.getLogger(MessageContainer.class);

	private final ObservableList<M> messages;
	private final int pageSize;
	private final int slidingWindowPages;

	private int lowPage;

	private int totalDataPages;

	private MessageClientRequest lastPreparation;

	public record MessageClientRequest(FetchMode fetchMode, int page, int size)
	{
	}

	public MessageContainer(ObservableList<M> messages, int pageSize, int slidingWindowPages)
	{
		if (pageSize <= 0)
		{
			throw new IllegalArgumentException("pageSize must be greater than 0");
		}
		if (slidingWindowPages <= 0)
		{
			throw new IllegalArgumentException("slidingWindowPages must be greater than 0");
		}
		this.messages = messages;
		this.pageSize = pageSize;
		this.slidingWindowPages = slidingWindowPages;
	}

	public void clear()
	{
		messages.clear();
		totalDataPages = 0;
		lowPage = 0;
	}

	/**
	 * Inserts a new message
	 *
	 * @param message a new incoming message
	 * @return true if the message has been inserted, false if it has updated an already existing entry
	 */
	public boolean insert(M message)
	{
		if (messages.contains(message))
		{
			return false;
		}

		var existingMessage = messages.stream()
				.filter(existing -> existing.getId() == message.getId() || existing.getId() == message.getOriginalId())
				.findFirst();
		if (existingMessage.isPresent())
		{
			messages.set(messages.indexOf(existingMessage.get()), message);
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

			if (size < slidingWindowPages)
			{
				messages.addLast(message);
				return true;
			}

			// We are after; no need to insert
		}
		return false;
	}

	/**
	 * Sets the read status of a message.
	 *
	 * @param messageId the message id, it can be null, in that case, every message in the group is concerned
	 * @param read      true if read
	 */
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

	public int getLowerBound()
	{
		return 0;
	}

	public int getHigherBound()
	{
		return messages.size() - 1;
	}

	public int getTotal()
	{
		return messages.size();
	}

	public void addBefore(List<M> messages)
	{
		this.messages.addAll(0, messages);
		trim();
	}

	public void addAfter(List<M> messages)
	{
		this.messages.addAll(messages);
		trim();
	}

	public void setTotalPages(int totalPages)
	{
		totalDataPages = totalPages;
	}

	public MessageClientRequest prepareFetchAll()
	{
		lastPreparation = new MessageClientRequest(FetchMode.ALL, 0, pageSize);
		return lastPreparation;
	}

	public MessageClientRequest prepareFetchBefore()
	{
		if (lowPage == 0)
		{
			return null;
		}
		lastPreparation = new MessageClientRequest(FetchMode.BEFORE, lowPage - 1, pageSize);
		return lastPreparation;
	}

	public MessageClientRequest prepareFetchAfter()
	{
		if (messages.size() < pageSize || (lowPage * pageSize + messages.size() + pageSize - 1) / pageSize >= totalDataPages)
		{
			return null;
		}
		lastPreparation = new MessageClientRequest(FetchMode.AFTER, lowPage + messages.size() / pageSize, pageSize);
		return lastPreparation;
	}

	private void trim()
	{
		int messagesToRemove;
		while ((messagesToRemove = messages.size() - pageSize * slidingWindowPages) >= pageSize)
		{
			log.debug("Trimming message size from {} to {}", messages.size(), pageSize * slidingWindowPages);

			// Find which side to chop off
			switch (lastPreparation.fetchMode())
			{
				case AFTER ->
				{
					log.debug("Trimming {} from beginning", messagesToRemove);
					messages.remove(0, messagesToRemove);
					lowPage++;
				}
				case BEFORE ->
				{
					log.debug("Trimming {} from end", messagesToRemove);
					messages.remove(messages.size() - messagesToRemove, messages.size());
					lowPage--;
				}
				case null, default -> log.error("Can't happen");
			}
		}
	}
}
