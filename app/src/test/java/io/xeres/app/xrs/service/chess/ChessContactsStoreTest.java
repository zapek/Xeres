/*
 * Copyright (c) 2019-2026 by David Gerber - https://zapek.com
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

package io.xeres.app.xrs.service.chess;

import io.xeres.common.dto.chess.ChessBoardDTO;
import io.xeres.common.dto.chess.ChessGameDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ChessContactsStoreTest
{
	@TempDir
	Path directory;

	private ChessHistoryStore historyStore;

	private static final String PEER_A = "aa".repeat(16);
	private static final String PEER_B = "bb".repeat(16);
	private static final String PEER_C = "cc".repeat(16);

	@BeforeEach
	void setUp()
	{
		historyStore = new ChessHistoryStore(directory.resolve("history"));
	}

	@Test
	void emptyOnFirstRunWithoutHistory()
	{
		var store = ChessContactsStore.forTesting(directory, historyStore);
		assertTrue(store.list().isEmpty());
		assertFalse(store.contains(PEER_A));
	}

	@Test
	void importPastOpponentsOnFirstRun() throws IOException
	{
		var board = new ChessPosition().squares();
		var game1 = new ChessGameDTO(PEER_A, "Alice", PEER_B, "CHECKMATE", true, false, board, "fen", "hash",
				List.of("e2e4"), List.of(), false, false, "", List.of(), false, false, false,
				List.of(new ChessBoardDTO(board, true, false), new ChessBoardDTO(board, false, false)));
		historyStore.save(UUID.randomUUID().toString(), "2026-03-01T12:00:00Z", "Bob", game1);

		var store = ChessContactsStore.forTesting(directory, historyStore);
		var contacts = store.list();
		assertEquals(2, contacts.size());
		assertTrue(store.contains(PEER_A));
		assertTrue(store.contains(PEER_B));

		// Second store instance on same directory should not re-import if a contact was deleted
		store.remove(PEER_A);
		assertFalse(store.contains(PEER_A));

		var store2 = ChessContactsStore.forTesting(directory, historyStore);
		assertFalse(store2.contains(PEER_A));
		assertTrue(store2.contains(PEER_B));
	}

	@Test
	void addAndRemoveContacts()
	{
		var store = ChessContactsStore.forTesting(directory, historyStore);
		assertTrue(store.add(PEER_C, "2026-03-01T15:00:00Z"));
		assertTrue(store.contains(PEER_C));
		assertEquals(1, store.list().size());

		// Adding existing contact returns false
		assertFalse(store.add(PEER_C, "2026-03-01T15:00:00Z"));

		// Persistence
		var reloaded = ChessContactsStore.forTesting(directory, historyStore);
		assertTrue(reloaded.contains(PEER_C));
		assertEquals("2026-03-01T15:00:00Z", reloaded.list().getFirst().lastSeen());

		// Remove
		assertTrue(reloaded.remove(PEER_C));
		assertFalse(reloaded.contains(PEER_C));
		assertTrue(reloaded.list().isEmpty());
	}

	@Test
	void filterOwnIdentity()
	{
		var identityService = org.mockito.Mockito.mock(io.xeres.app.service.IdentityService.class);
		var ownIdentity = org.mockito.Mockito.mock(io.xeres.app.xrs.service.identity.item.IdentityGroupItem.class);
		org.mockito.Mockito.when(identityService.hasOwnIdentity()).thenReturn(true);
		org.mockito.Mockito.when(identityService.getOwnIdentity()).thenReturn(ownIdentity);
		org.mockito.Mockito.when(ownIdentity.getGxsId()).thenReturn(io.xeres.common.id.GxsId.fromString(PEER_A));

		var store = ChessContactsStore.forTesting(directory, historyStore, identityService);
		assertFalse(store.add(PEER_A));
		assertTrue(store.add(PEER_B));
		assertEquals(1, store.list().size());
		assertFalse(store.contains(PEER_A));
		assertTrue(store.contains(PEER_B));
	}
}