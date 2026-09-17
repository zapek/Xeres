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

import io.xeres.app.application.environment.DataDirLocator;
import io.xeres.common.dto.chess.ChessGameDTO;
import io.xeres.common.dto.chess.ChessHistorySummaryDTO;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@Service
public class ChessHistoryStore
{
	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ChessHistoryStore.class);
	private final Path directory;

	public ChessHistoryStore()
	{
		directory = null;
	}

	ChessHistoryStore(Path directory)
	{
		this.directory = directory;
	}

	private Path directory()
	{
		if (directory != null) return directory;
		var data = DataDirLocator.getDataDir();
		if (data == null) throw new IllegalStateException("Chess history data directory is unavailable");
		return Path.of(data, "chess-history");
	}

	public synchronized void save(String id, String startedAt, String localName, ChessGameDTO game) throws IOException
	{
		var folder = directory();
		Files.createDirectories(folder);
		var target = folder.resolve(UUID.fromString(id) + ".json");
		var temporary = Files.createTempFile(folder, ".saving-", ".tmp");
		try
		{
			var bytes = JsonMapper.builder().build().writeValueAsBytes(new SavedGame(startedAt, localName, game));
			Files.write(temporary, bytes);
			try
			{
				Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			}
			catch (AtomicMoveNotSupportedException ignored)
			{
				Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
			}
		}
		finally
		{
			Files.deleteIfExists(temporary);
		}
	}

	public synchronized List<ChessHistorySummaryDTO> list() throws IOException
	{
		var folder = directory();
		if (!Files.exists(folder)) return List.of();
		var result = new ArrayList<ChessHistorySummaryDTO>();
		try (var files = Files.newDirectoryStream(folder, "*.json"))
		{
			for (var file : files)
			{
				try
				{
					var id = UUID.fromString(file.getFileName().toString().replace(".json", "")).toString();
					var saved = read(id);
					var game = saved.game();
					result.add(new ChessHistorySummaryDTO(id, saved.startedAt(), game.white() ? saved.localName() : game.name(), game.white() ? game.name() : saved.localName(), game.status(), game.moves().size(), game.white() ? game.localIdentity() : game.peer(), game.white() ? game.peer() : game.localIdentity()));
				}
				catch (IOException | RuntimeException failure)
				{
					log.warn("Cannot read chess history {}", file.getFileName(), failure);
				}
			}
		}
		result.sort(Comparator.comparing(ChessHistorySummaryDTO::startedAt).reversed());
		return List.copyOf(result);
	}

	public synchronized ChessGameDTO load(String id) throws IOException
	{
		return read(id).game();
	}

	private SavedGame read(String id) throws IOException
	{
		var path = directory().resolve(UUID.fromString(id) + ".json");
		try (var input = Files.newInputStream(path))
		{
			var data = input.readNBytes(8_000_001);
			if (data.length > 8_000_000) throw new IOException("Chess history is too large");
			var saved = JsonMapper.builder().build().readValue(data, SavedGame.class);
			if (saved == null || saved.startedAt() == null || saved.game() == null || saved.game().moves() == null ||
					saved.game().positions() == null || saved.game().positions().size() != saved.game().moves().size() + 1)
			{
				throw new IOException("Invalid chess history");
			}
			return saved;
		}
	}

	private record SavedGame(String startedAt, String localName, ChessGameDTO game)
	{
	}
}
