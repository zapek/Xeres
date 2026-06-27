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

import io.xeres.ui.model.board.BoardMessage;
import javafx.collections.FXCollections;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessageContainerTest
{
	private static List<BoardMessage> createBoardMessages(int size)
	{
		List<BoardMessage> arrayList = new ArrayList<>(size);
		for (int i = 0; i < size; i++)
		{
			arrayList.add(new BoardMessage()); // XXX: beware, we should set some message id and instant if we start inserting and stuff
		}
		return arrayList;
	}

	@Test
	void OneMessage()
	{
		var messageContainer = new MessageContainer<BoardMessage>(FXCollections.observableArrayList(), 20, 3);
		messageContainer.insert(new BoardMessage());
		assertEquals(0, messageContainer.getLowerBound());
		assertEquals(0, messageContainer.getHigherBound());
		assertEquals(1, messageContainer.getTotal());
	}

	@Test
	void addSeveralPages()
	{
		final var PAGE_SIZE = 20;
		final var SLIDING_WINDOW_SIZE = 3;

		var messageContainer = new MessageContainer<BoardMessage>(FXCollections.observableArrayList(), PAGE_SIZE, SLIDING_WINDOW_SIZE);
		messageContainer.setTotalPages(101);

		// Get all (20)
		var messageClientRequest = messageContainer.prepareFetchAll();
		assertEquals(FetchMode.ALL, messageClientRequest.fetchMode());
		assertEquals(0, messageClientRequest.page());
		assertEquals(PAGE_SIZE, messageClientRequest.size());
		messageContainer.addAfter(createBoardMessages(PAGE_SIZE));
		assertEquals(PAGE_SIZE, messageContainer.getTotal());

		// Next page (40)
		messageClientRequest = messageContainer.prepareFetchAfter();
		assertEquals(FetchMode.AFTER, messageClientRequest.fetchMode());
		assertEquals(1, messageClientRequest.page());
		assertEquals(PAGE_SIZE, messageClientRequest.size());
		messageContainer.addAfter(createBoardMessages(PAGE_SIZE));
		assertEquals(PAGE_SIZE * 2, messageContainer.getTotal());

		// Next page (60, full)
		messageClientRequest = messageContainer.prepareFetchAfter();
		assertEquals(FetchMode.AFTER, messageClientRequest.fetchMode());
		assertEquals(2, messageClientRequest.page());
		assertEquals(PAGE_SIZE, messageClientRequest.size());
		messageContainer.addAfter(createBoardMessages(PAGE_SIZE));
		assertEquals(PAGE_SIZE * 3, messageContainer.getTotal());

		// Next page (80, clear first 20)
		messageClientRequest = messageContainer.prepareFetchAfter();
		assertEquals(FetchMode.AFTER, messageClientRequest.fetchMode());
		assertEquals(3, messageClientRequest.page());
		assertEquals(PAGE_SIZE, messageClientRequest.size());
		messageContainer.addAfter(createBoardMessages(PAGE_SIZE));
		assertEquals(PAGE_SIZE * 3, messageContainer.getTotal());

		// Next page (100, clear first 20)
		messageClientRequest = messageContainer.prepareFetchAfter();
		assertEquals(FetchMode.AFTER, messageClientRequest.fetchMode());
		assertEquals(4, messageClientRequest.page());
		assertEquals(PAGE_SIZE, messageClientRequest.size());
		messageContainer.addAfter(createBoardMessages(PAGE_SIZE));
		assertEquals(PAGE_SIZE * 3, messageContainer.getTotal());

		// Next page (101, clear first 1)
		messageClientRequest = messageContainer.prepareFetchAfter();
		assertEquals(FetchMode.AFTER, messageClientRequest.fetchMode());
		assertEquals(5, messageClientRequest.page());
		assertEquals(PAGE_SIZE, messageClientRequest.size());
		messageContainer.addAfter(createBoardMessages(1));
		assertEquals(PAGE_SIZE * 3 + 1, messageContainer.getTotal());

		// Previous page (20, clear last)
		messageClientRequest = messageContainer.prepareFetchBefore();
		assertEquals(FetchMode.BEFORE, messageClientRequest.fetchMode());
		assertEquals(1, messageClientRequest.page());
		assertEquals(PAGE_SIZE, messageClientRequest.size());
		messageContainer.addAfter(createBoardMessages(PAGE_SIZE));
		assertEquals(PAGE_SIZE * 3, messageContainer.getTotal());
	}
}