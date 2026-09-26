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

import io.xeres.ui.plugin.*;
import io.xeres.ui.client.message.MessageClient;
import io.xeres.chess.ui.client.message.ChessFrameHandler;
import io.xeres.chess.ui.controller.chess.ChessPageController;
import io.xeres.chess.ui.controller.settings.SettingsChessController;
import io.xeres.chess.ui.support.chess.ChessWindowService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import java.util.List;
import java.util.ResourceBundle;

import static io.xeres.common.message.MessagePath.APP_PREFIX;
import static io.xeres.chess.ChessPaths.*;

@Component
@ConditionalOnProperty(value = "xeres.plugins.chess.enabled", havingValue = "true")
public class ChessUiPlugin implements UiPlugin
{
	private final ChessWindowService windows;
	private final ObjectProvider<ChessPageController> page;
	private final ResourceBundle bundle;

	public ChessUiPlugin(ChessWindowService windows, ObjectProvider<ChessPageController> page, ResourceBundle bundle)
	{
		this.windows = windows;
		this.page = page;
		this.bundle = io.xeres.chess.ChessResources.bundle(bundle);
	}

	@Override
	public List<PluginTab> tabs()
	{
		return List.of(new PluginTab("chess", bundle.getString("main.chess"), "mdi2c-chess-knight",
				ChessPageController.class, bundle, () -> page.getObject().refreshOnTabSelection()));
	}

	@Override
	public List<PluginSettingsPage> settingsPages()
	{
		return List.of(new PluginSettingsPage(bundle.getString("settings.chess"), "mdi2c-chess-knight", SettingsChessController.class, bundle));
	}

	@Override
	public List<PluginIdentityAction> identityActions()
	{
		return List.of(new PluginIdentityAction("chess.invite", bundle.getString("chess.invite"), "mdi2c-chess-knight", windows::inviteChess));
	}

	@Override
	public void subscribe(MessageClient messages)
	{
		messages.subscribe(chessDestination(), new ChessFrameHandler(windows))
				.subscribe(APP_PREFIX + CHESS_ROOT, new ChessFrameHandler(windows));
	}

	@Override
	public void onExit()
	{
		windows.close();
	}
}
