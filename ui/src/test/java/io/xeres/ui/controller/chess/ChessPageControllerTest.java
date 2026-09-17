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

package io.xeres.ui.controller.chess;

import io.xeres.common.dto.chess.ChessLeaderboardEntryDTO;
import io.xeres.ui.client.ChessClient;
import io.xeres.ui.client.ContactClient;
import io.xeres.ui.client.GeneralClient;
import io.xeres.ui.client.IdentityClient;
import io.xeres.ui.custom.asyncimage.ImageCache;
import io.xeres.ui.support.chess.ChessSettings;
import io.xeres.ui.support.own.OwnCache;
import io.xeres.ui.support.sound.SoundPlayerService;
import io.xeres.ui.support.window.WindowManager;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.TabPane;
import org.junit.jupiter.api.Test;
import org.kordamp.ikonli.javafx.FontIcon;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testfx.framework.junit5.ApplicationExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith({ApplicationExtension.class, MockitoExtension.class})
class ChessPageControllerTest
{
	@Mock
	private ChessClient chessClient;

	@Mock
	private ContactClient contactClient;

	@Mock
	private IdentityClient identityClient;

	@Mock
	private GeneralClient generalClient;

	@Mock
	private ImageCache imageCache;

	@Mock
	private WindowManager windowManager;

	@Mock
	private SoundPlayerService soundPlayer;

	@Mock
	private ChessSettings chessSettings;

	@Mock
	private OwnCache ownCache;

	@Test
	void chessPageLoadsInEverySupportedLanguage() throws IOException
	{
		org.mockito.Mockito.lenient().when(chessClient.games()).thenReturn(Mono.just(List.of()));
		org.mockito.Mockito.lenient().when(chessClient.contacts()).thenReturn(Mono.just(List.of()));
		org.mockito.Mockito.lenient().when(chessClient.isBusy()).thenReturn(Mono.just(false));
		org.mockito.Mockito.lenient().when(chessClient.history()).thenReturn(Mono.just(List.of()));
		org.mockito.Mockito.lenient().when(chessClient.leaderboard()).thenReturn(Mono.just(List.of(
				new ChessLeaderboardEntryDTO(1, "11".repeat(16), "Alice", 1724, 153, 7, 1, 5, 1, "Provisional", "2026-09-17T18:00:00Z")
		)));
		org.mockito.Mockito.lenient().when(contactClient.getContacts()).thenReturn(Flux.empty());
		org.mockito.Mockito.lenient().when(identityClient.getIdentities()).thenReturn(Flux.empty());
		org.mockito.Mockito.lenient().when(ownCache.getProfileName()).thenReturn("Local");

		for (var language : List.of("en", "fr", "es", "ru", "zh"))
		{
			var bundle = ResourceBundle.getBundle("i18n.messages", Locale.forLanguageTag(language));
			var controller = new ChessPageController(
					chessClient, contactClient, identityClient, generalClient, imageCache,
					windowManager, soundPlayer, chessSettings, bundle, ownCache
			);

			var loader = new FXMLLoader(getClass().getResource("/view/chess/chess_page.fxml"), bundle);
			loader.setControllerFactory(_ -> controller);
			Parent root = loader.load();
			assertNotNull(root);

			var tabPane = (TabPane) root.lookup("#tabPane");
			assertNotNull(tabPane);
			assertEquals(3, tabPane.getTabs().size());

			var onlineFilterButton = controller.getOnlineFilterButton();
			assertNotNull(onlineFilterButton);
			var onlineIcon = (FontIcon) onlineFilterButton.getGraphic();
			assertNotNull(onlineIcon);
			assertEquals("mdi2a-account-check", onlineIcon.getIconLiteral());

			var addContactButton = controller.getAddContactButton();
			assertNotNull(addContactButton);
			var addIcon = (FontIcon) addContactButton.getGraphic();
			assertNotNull(addIcon);
			assertEquals("mdi2p-plus", addIcon.getIconLiteral());

			controller.start();
			controller.stop();
		}
	}
}
