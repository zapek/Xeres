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
import io.xeres.app.service.IdentityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@Service
public class ChessContactsStore
{
	private static final Logger log = LoggerFactory.getLogger(ChessContactsStore.class);
	private static final String FILE_NAME = "chess-contacts.json";

	private Path filePath;
	private final ChessHistoryStore historyStore;
	private final IdentityService identityService;
	private final Map<String, SavedContact> contacts = new LinkedHashMap<>();
	private boolean loaded;

	public record SavedContact(String gxsId, String lastSeen)
	{
		public SavedContact(String gxsId)
		{
			this(gxsId, "");
		}
	}

	public ChessContactsStore(ChessHistoryStore historyStore, IdentityService identityService)
	{
		this.historyStore = historyStore;
		this.identityService = identityService;
	}

	public static ChessContactsStore forTesting(Path folder, ChessHistoryStore historyStore)
	{
		return forTesting(folder, historyStore, null);
	}

	public static ChessContactsStore forTesting(Path folder, ChessHistoryStore historyStore, IdentityService identityService)
	{
		var store = new ChessContactsStore(historyStore, identityService);
		store.filePath = folder.resolve(FILE_NAME);
		return store;
	}

	private String getOwnGxsId()
	{
		if (identityService != null && identityService.hasOwnIdentity())
		{
			var own = identityService.getOwnIdentity().getGxsId();
			if (own != null)
			{
				return own.asString();
			}
		}
		return null;
	}

	private Path file()
	{
		if (filePath != null)
		{
			return filePath;
		}
		var data = DataDirLocator.getDataDir();
		if (data == null)
		{
			throw new IllegalStateException("Data directory is unavailable");
		}
		return Path.of(data, FILE_NAME);
	}

	private synchronized void ensureLoaded()
	{
		if (loaded)
		{
			return;
		}
		loaded = true;
		var path = file();
		if (Files.exists(path))
		{
			try (var input = Files.newInputStream(path))
			{
				var data = input.readNBytes(2_000_000);
				if (data.length > 0)
				{
					List<SavedContact> list = JsonMapper.builder().build().readValue(data, new TypeReference<>() {});
					if (list != null)
					{
						var own = getOwnGxsId();
						for (var item : list)
						{
							if (item != null && item.gxsId() != null && !item.gxsId().isBlank())
							{
								if (own == null || !item.gxsId().equalsIgnoreCase(own))
								{
									contacts.put(item.gxsId(), item);
								}
							}
						}
					}
				}
			}
			catch (IOException | RuntimeException e)
			{
				log.warn("Failed to read chess contacts from {}", path, e);
			}
		}
		else
		{
			// First run: import past opponents from game history if available
			importHistoryOpponents();
			saveToFile();
		}

		var own = getOwnGxsId();
		if (own != null && contacts.containsKey(own))
		{
			contacts.remove(own);
			saveToFile();
		}
	}

	private void importHistoryOpponents()
	{
		if (historyStore == null)
		{
			return;
		}
		try
		{
			var own = getOwnGxsId();
			var games = historyStore.list();
			for (var game : games)
			{
				if (game.whiteIdentity() != null && !game.whiteIdentity().isBlank()
						&& (own == null || !game.whiteIdentity().equalsIgnoreCase(own)))
				{
					contacts.putIfAbsent(game.whiteIdentity(), new SavedContact(game.whiteIdentity(), game.startedAt()));
				}
				if (game.blackIdentity() != null && !game.blackIdentity().isBlank()
						&& (own == null || !game.blackIdentity().equalsIgnoreCase(own)))
				{
					contacts.putIfAbsent(game.blackIdentity(), new SavedContact(game.blackIdentity(), game.startedAt()));
				}
			}
		}
		catch (IOException | RuntimeException e)
		{
			log.warn("Unable to import past chess opponents from history", e);
		}
	}

	private synchronized void saveToFile()
	{
		var path = file();
		try
		{
			var parent = path.getParent();
			if (parent != null)
			{
				Files.createDirectories(parent);
			}
			var temporary = Files.createTempFile(parent != null ? parent : Path.of("."), ".chess-contacts-", ".tmp");
			try
			{
				var bytes = JsonMapper.builder().build().writeValueAsBytes(new ArrayList<>(contacts.values()));
				Files.write(temporary, bytes);
				try
				{
					Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
				}
				catch (AtomicMoveNotSupportedException ignored)
				{
					Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
				}
			}
			finally
			{
				Files.deleteIfExists(temporary);
			}
		}
		catch (IOException e)
		{
			log.error("Unable to save chess contacts to {}", path, e);
		}
	}

	public synchronized List<SavedContact> list()
	{
		ensureLoaded();
		var own = getOwnGxsId();
		if (own != null)
		{
			return contacts.values().stream()
					.filter(c -> !c.gxsId().equalsIgnoreCase(own))
					.toList();
		}
		return List.copyOf(contacts.values());
	}

	public synchronized Set<String> getGxsIds()
	{
		ensureLoaded();
		var own = getOwnGxsId();
		if (own != null)
		{
			return contacts.keySet().stream()
					.filter(id -> !id.equalsIgnoreCase(own))
					.collect(java.util.stream.Collectors.toSet());
		}
		return Set.copyOf(contacts.keySet());
	}

	public synchronized boolean contains(String gxsId)
	{
		if (gxsId == null || gxsId.isBlank())
		{
			return false;
		}
		var own = getOwnGxsId();
		if (own != null && gxsId.equalsIgnoreCase(own))
		{
			return false;
		}
		ensureLoaded();
		return contacts.containsKey(gxsId);
	}

	public synchronized Optional<String> getLastSeen(String gxsId)
	{
		if (gxsId == null || gxsId.isBlank())
		{
			return Optional.empty();
		}
		ensureLoaded();
		var contact = contacts.get(gxsId);
		return contact != null && contact.lastSeen() != null && !contact.lastSeen().isBlank()
				? Optional.of(contact.lastSeen())
				: Optional.empty();
	}

	public synchronized boolean add(String gxsId)
	{
		return add(gxsId, "");
	}

	public synchronized boolean add(String gxsId, String lastSeen)
	{
		if (gxsId == null || gxsId.isBlank())
		{
			return false;
		}
		var own = getOwnGxsId();
		if (own != null && gxsId.equalsIgnoreCase(own))
		{
			return false;
		}
		ensureLoaded();
		var existing = contacts.get(gxsId);
		if (existing != null)
		{
			if (lastSeen != null && !lastSeen.isBlank() && !lastSeen.equals(existing.lastSeen()))
			{
				contacts.put(gxsId, new SavedContact(gxsId, lastSeen));
				saveToFile();
			}
			return false;
		}
		contacts.put(gxsId, new SavedContact(gxsId, lastSeen != null ? lastSeen : ""));
		saveToFile();
		return true;
	}

	public synchronized boolean remove(String gxsId)
	{
		if (gxsId == null || gxsId.isBlank())
		{
			return false;
		}
		ensureLoaded();
		if (contacts.remove(gxsId) != null)
		{
			saveToFile();
			return true;
		}
		return false;
	}
}