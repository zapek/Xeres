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

package io.xeres.chess;

import io.xeres.plugin.XeresPlugin;
import io.xeres.chess.app.api.controller.chess.ChessController;
import io.xeres.chess.app.api.controller.chess.ChessMessageController;
import io.xeres.chess.app.xrs.service.chess.ChessContactsStore;
import io.xeres.chess.app.xrs.service.chess.ChessHistoryStore;
import io.xeres.chess.app.xrs.service.chess.ChessRatingService;
import io.xeres.chess.app.xrs.service.chess.ChessRsService;

import java.util.List;

public class ChessPlugin implements XeresPlugin
{
	@Override
	public String id()
	{
		return "chess";
	}

	@Override
	public int apiVersion()
	{
		return API_VERSION;
	}

	@Override
	public List<Class<?>> components()
	{
		return List.of(ChessRsService.class, ChessContactsStore.class, ChessHistoryStore.class,
				ChessRatingService.class, ChessController.class, ChessMessageController.class);
	}
	@Override
	public List<Class<?>> uiComponents()
	{
		return List.of(ChessUiPlugin.class,
				io.xeres.chess.ui.client.ChessClient.class,
				io.xeres.chess.ui.support.chess.ChessSettings.class,
				io.xeres.chess.ui.support.chess.ChessSoundService.class,
				io.xeres.chess.ui.support.chess.ChessWindowService.class,
				io.xeres.chess.ui.controller.chess.ChessPageController.class,
				io.xeres.chess.ui.controller.settings.SettingsChessController.class);
	}
}
