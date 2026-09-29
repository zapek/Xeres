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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ChessHistoryStoreTest
{
	@TempDir Path directory;

	private ChessGameDTO game()
	{
		var board = new ChessPosition().squares();
		return new ChessGameDTO("22".repeat(16), "Opponent", "11".repeat(16), "ACTIVE", true,
				true, board, "fen", "hash", List.of(), List.of(), false, false, "", List.of(), false,
				false, false, List.of(new ChessBoardDTO(board, true, false)));
	}

	@Test
	void savesAutomaticallyAndSurvivesStoreRestart() throws Exception
	{
		var id = UUID.randomUUID().toString();
		var store = new ChessHistoryStore(directory);
		store.save(id, "2026-09-11T18:00:00Z", "Local", game());
		store.save(id, "2026-09-11T18:00:00Z", "Local", game());
		var reopened = new ChessHistoryStore(directory);
		assertEquals(game(), reopened.load(id));
		assertEquals(1, reopened.list().size());
		assertEquals("Local", reopened.list().getFirst().whiteName());
		assertEquals("Opponent", reopened.list().getFirst().blackName());
		assertEquals("11".repeat(16), reopened.list().getFirst().whiteIdentity());
		assertEquals("22".repeat(16), reopened.list().getFirst().blackIdentity());
		var rematch = UUID.randomUUID().toString();
		store.save(rematch, "2026-09-11T19:00:00Z", "Local", game());
		assertEquals(2, reopened.list().size());
		assertEquals(rematch, reopened.list().getFirst().id());
		assertEquals(game(), reopened.load(id));
	}

	@Test
	void rejectsTraversalAndSkipsDamagedHistory() throws Exception
	{
		var store = new ChessHistoryStore(directory);
		Files.writeString(directory.resolve(UUID.randomUUID() + ".json"), "{}");
		assertTrue(store.list().isEmpty());
		assertThrows(IllegalArgumentException.class, () -> store.load("../other"));
	}
}
