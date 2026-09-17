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

package io.xeres.app.api.controller.chess;

import io.xeres.app.xrs.service.chess.ChessRsService;
import io.xeres.common.dto.chess.ChessGameDTO;
import io.xeres.common.id.GxsId;
import io.xeres.common.rest.chess.ChessActionRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import io.xeres.app.service.IdentityService;
import io.xeres.app.xrs.service.chess.ChessContactsStore;
import io.xeres.app.xrs.service.chess.ChessHistoryStore;
import io.xeres.app.xrs.service.chess.ChessRatingService;
import io.xeres.common.dto.chess.ChessContactDTO;
import io.xeres.common.dto.chess.ChessHistorySummaryDTO;
import io.xeres.common.dto.chess.ChessLeaderboardEntryDTO;

import java.util.List;

import static io.xeres.common.rest.PathConfig.CHESS_PATH;

@RestController
@RequestMapping(CHESS_PATH)
public class ChessController
{
	private final ChessRsService chess;
	private final ChessHistoryStore history;
	private final ChessRatingService ratingService;
	private final ChessContactsStore contactsStore;
	private final IdentityService identityService;

	public ChessController(ChessRsService chess, ChessHistoryStore history, ChessRatingService ratingService, ChessContactsStore contactsStore, IdentityService identityService)
	{
		this.chess = chess;
		this.history = history;
		this.ratingService = ratingService;
		this.contactsStore = contactsStore;
		this.identityService = identityService;
	}

	@GetMapping("/leaderboard")
	public List<io.xeres.common.dto.chess.ChessLeaderboardEntryDTO> leaderboard()
	{
		return ratingService.getLeaderboard();
	}

	@GetMapping("/ratings/{peer}")
	public ChessLeaderboardEntryDTO rating(@PathVariable String peer)
	{
		return ratingService.getRating(peer);
	}

	@GetMapping("/contacts")
	public List<ChessContactDTO> contacts()
	{
		return chess.contacts();
	}

	@GetMapping("/busy")
	public boolean isBusy()
	{
		return chess.isBusy();
	}

	@PostMapping("/busy")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void setBusy(@RequestParam boolean busy)
	{
		chess.setBusy(busy);
	}

	@PostMapping("/contacts/{peer}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void addContact(@PathVariable String peer)
	{
		var id = identity(peer);
		if (identityService.hasOwnIdentity() && id.equals(identityService.getOwnIdentity().getGxsId()))
		{
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot add own identity as chess contact");
		}
		contactsStore.add(id.asString());
	}

	@DeleteMapping("/contacts/{peer}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void removeContact(@PathVariable String peer)
	{
		var id = identity(peer);
		contactsStore.remove(id.asString());
	}


	@GetMapping
	public List<ChessGameDTO> list()
	{
		return chess.list();
	}

	@GetMapping("/history")
	public List<io.xeres.common.dto.chess.ChessHistorySummaryDTO> history() throws java.io.IOException
	{
		return history.list();
	}

	@GetMapping("/history/{id}")
	public ChessGameDTO history(@PathVariable java.util.UUID id) throws java.io.IOException
	{
		return history.load(id.toString());
	}

	@PostMapping("/{peer}/invite")
	public ChessGameDTO invite(@PathVariable String peer)
	{
		return chess.invite(identity(peer));
	}

	@PostMapping("/{peer}/actions")
	public ChessGameDTO action(@PathVariable String peer, @Valid @RequestBody ChessActionRequest request)
	{
		return chess.action(identity(peer), request.action());
	}

	private GxsId identity(String peer)
	{
		var id = GxsId.fromString(peer);
		if (id.isNullIdentifier())
		{
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid identity");
		}
		return id;
	}

}
