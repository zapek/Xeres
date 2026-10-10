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

package io.xeres.chess.ui.support.chess;

import io.xeres.common.id.GxsId;
import io.xeres.ui.client.*;
import io.xeres.chess.ui.client.ChessClient;
import io.xeres.ui.custom.asyncimage.ImageCache;
import io.xeres.ui.support.own.OwnCache;
import io.xeres.ui.support.util.Requester;
import io.xeres.ui.support.window.WindowManager;
import io.xeres.ui.support.window.WindowManager.UiWindow;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;
import java.util.*;

import static io.xeres.ui.support.window.WindowManager.getOpenedWindow;
import io.xeres.chess.ui.support.chess.ChessSoundService.SoundType;

@Service
public class ChessWindowService
{
	private final ChessClient chessClient;
	private final GeneralClient generalClient;
	private final IdentityClient identityClient;
	private final ImageCache imageCache;
	private final OwnCache ownCache;
	private final ChessSettings chessSettings;
	private final ChessSoundService soundPlayerService;
	private final ResourceBundle bundle;
	private final WindowManager windowManager;
	private final Set<String> shownChessInvitations = new HashSet<>();
	private final Map<String, ChessInvitationToaster> activeChessToasters = new java.util.concurrent.ConcurrentHashMap<>();

	public ChessWindowService(ChessClient chessClient, GeneralClient generalClient, IdentityClient identityClient,
			ImageCache imageCache, OwnCache ownCache, ChessSettings chessSettings, ChessSoundService soundPlayerService,
			ResourceBundle bundle, WindowManager windowManager)
	{
		this.chessClient = chessClient;
		this.generalClient = generalClient;
		this.identityClient = identityClient;
		this.imageCache = imageCache;
		this.ownCache = ownCache;
		this.chessSettings = chessSettings;
		this.soundPlayerService = soundPlayerService;
		this.bundle = io.xeres.chess.ChessResources.bundle(bundle);
		this.windowManager = windowManager;
	}

	public void close()
	{
		activeChessToasters.values().forEach(ChessInvitationToaster::close);
		activeChessToasters.clear();
	}

	public void inviteChess(GxsId peer)
	{
		chessClient.invite(peer.asString()).subscribe(game -> Platform.runLater(() -> openChess(game)),
				failure -> Platform.runLater(() -> Requester.showError(bundle.getString("chess.error") + " " + failure.getMessage())));
	}

	@EventListener
	public void updateChess(io.xeres.chess.ui.event.ChessGamesEvent event)
	{
		Platform.runLater(() -> {
			shownChessInvitations.retainAll(event.games().stream()
					.filter(game -> game.status().equals("INCOMING")).map(io.xeres.chess.common.dto.chess.ChessGameDTO::peer).toList());

			// Close any active toasters for games that are no longer INCOMING
			activeChessToasters.entrySet().removeIf(entry -> {
				boolean stillIncoming = event.games().stream()
						.anyMatch(g -> g.peer().equals(entry.getKey()) && "INCOMING".equals(g.status()));
				if (!stillIncoming)
				{
					entry.getValue().close();
					return true;
				}
				return false;
			});

			for (var game : event.games())
			{
				getOpenedWindow(io.xeres.chess.ui.controller.chess.ChessWindowController.class, game.peer())
						.ifPresent(window -> ((io.xeres.chess.ui.controller.chess.ChessWindowController) window.getUserData()).update(game));
				if (game.status().equals("INCOMING"))
				{
					var windowOpt = getOpenedWindow(io.xeres.chess.ui.controller.chess.ChessWindowController.class, game.peer())
							.filter(Window::isShowing);
					if (windowOpt.isEmpty())
					{
						if (shownChessInvitations.add(game.peer()))
						{
							if (chessSettings.isNotificationEnabled())
							{
								showChessToaster(game);
							}
							else
							{
								openChess(game);
								soundPlayerService.play(SoundType.CHESS_INVITE);
							}
						}
					}
					else
					{
						shownChessInvitations.add(game.peer());
						var stage = (Stage) windowOpt.get();
						if (stage.isIconified())
						{
							stage.setIconified(false);
						}
						stage.toFront();
						stage.requestFocus();
					}
				}
			}
		});
	}

	private void showChessToaster(io.xeres.chess.common.dto.chess.ChessGameDTO game)
	{
		if (activeChessToasters.containsKey(game.peer()))
		{
			return;
		}
		int stackIndex = activeChessToasters.size();
		var toaster = new io.xeres.chess.ui.support.chess.ChessInvitationToaster(
				game,
				chessClient,
				generalClient,
				imageCache,
				bundle,
				stackIndex,
				this::openChess,
				() -> activeChessToasters.remove(game.peer())
		);
		activeChessToasters.put(game.peer(), toaster);
		toaster.show();
		soundPlayerService.play(SoundType.CHESS_INVITE);
	}

	public void openChessHistory()
	{
		io.xeres.chess.ui.controller.chess.ChessWindowController.browseHistory(null, chessClient, bundle, soundPlayerService, chessSettings, generalClient, imageCache);
	}

	public void openChessPage()
	{
		windowManager.openPluginPage("chess");
	}

	public void openChessSettings()
	{
		windowManager.openSettings(io.xeres.chess.ui.controller.settings.SettingsChessController.class);
	}

	public void openChess(io.xeres.chess.common.dto.chess.ChessGameDTO game)
	{
		var toaster = activeChessToasters.remove(game.peer());
		if (toaster != null)
		{
			toaster.close();
		}
		getOpenedWindow(io.xeres.chess.ui.controller.chess.ChessWindowController.class, game.peer())
				.filter(Window::isShowing)
				.ifPresentOrElse(
						window -> {
							var stage = (Stage) window;
							if (stage.isIconified())
							{
								stage.setIconified(false);
							}
							((io.xeres.chess.ui.controller.chess.ChessWindowController) window.getUserData()).update(game);
							stage.toFront();
							stage.requestFocus();
						},
						() -> {
							var controller = new io.xeres.chess.ui.controller.chess.ChessWindowController(chessClient, bundle, game, soundPlayerService, chessSettings);
							var window = UiWindow.builder("/view/chess/chess_window.fxml", controller, bundle)
									.setLocalId(game.peer()).setTitle(MessageFormat.format(bundle.getString("chess.window-title"), game.name())).build();
							controller.setOpenSettingsAction(this::openChessSettings);
							controller.showPlayerProfiles(generalClient, imageCache, identityClient, ownCache.getProfileName());
							window.open();
						});
	}

	public void watchChess(io.xeres.chess.common.dto.chess.ChessActiveGameDTO match)
	{
		getOpenedWindow(io.xeres.chess.ui.controller.chess.ChessWatchWindowController.class, match.host())
				.filter(Window::isShowing).ifPresentOrElse(window -> {
					var stage = (Stage) window;
					((io.xeres.chess.ui.controller.chess.ChessWatchWindowController) stage.getUserData()).showMatch(match);
					stage.setTitle(bundle.getString("chess.watch.title") + ": " + match.playerName() + " vs " + match.opponentName());
					stage.setIconified(false);
					stage.toFront();
					stage.requestFocus();
				}, () -> {
					var controller = new io.xeres.chess.ui.controller.chess.ChessWatchWindowController(chessClient, match, bundle, chessSettings);
					UiWindow.builder("/view/chess/chess_watch_window.fxml", controller, bundle)
							.setLocalId(match.host()).setTitle(bundle.getString("chess.watch.title") + ": " + match.playerName() + " vs " + match.opponentName())
							.build().open();
				});
	}

}
