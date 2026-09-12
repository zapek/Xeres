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

package io.xeres.ui.client.message;

import io.xeres.common.dto.chess.ChessGameDTO;
import io.xeres.ui.event.ChessGamesEvent;
import io.xeres.ui.support.window.WindowManager;
import org.jspecify.annotations.NonNull;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;

import java.lang.reflect.Type;
import java.util.List;

public class ChessFrameHandler implements StompFrameHandler
{
	private final WindowManager windowManager;

	public ChessFrameHandler(WindowManager windowManager)
	{
		this.windowManager = windowManager;
	}

	@Override
	public @NonNull Type getPayloadType(StompHeaders headers)
	{
		return ChessGameDTO[].class;
	}

	@Override
	public void handleFrame(StompHeaders headers, Object payload)
	{
		windowManager.updateChess(new ChessGamesEvent(List.of((ChessGameDTO[]) payload)));
	}
}