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

import java.util.List;

import static io.xeres.common.rest.PathConfig.CHESS_PATH;

@RestController
@RequestMapping(CHESS_PATH)
public class ChessController
{
	private final ChessRsService chess;

	public ChessController(ChessRsService chess)
	{
		this.chess = chess;
	}

	@GetMapping
	public List<ChessGameDTO> list()
	{
		return chess.list();
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
		if (!peer.matches("[0-9a-fA-F]{32}") || peer.equals("0".repeat(32)))
		{
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid identity");
		}
		return GxsId.fromString(peer);
	}

	@ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
	@ResponseStatus(HttpStatus.CONFLICT)
	public org.springframework.http.ProblemDetail invalidAction(RuntimeException exception)
	{
		return org.springframework.http.ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
	}
}
