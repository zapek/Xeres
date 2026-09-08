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

package io.xeres.ui.controller.settings;

import io.xeres.ui.FXTest;
import io.xeres.ui.support.chess.ChessBoardTheme;
import io.xeres.ui.support.chess.ChessSettings;
import io.xeres.ui.support.sound.SoundPlayerService;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.TilePane;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SettingsChessControllerTest extends FXTest
{
	@Test
	void themesSoundsAndSaveWorkInEveryLanguage() throws Exception
	{
		var done = new CompletableFuture<Void>();
		Platform.runLater(() -> {
			try
			{
				for (var language : List.of("en", "fr", "es", "ru", "zh"))
				{
					var settings = mock(ChessSettings.class);
					when(settings.getTheme()).thenReturn(ChessBoardTheme.BROWN);
					when(settings.isMoveEnabled()).thenReturn(true);
					when(settings.isCaptureEnabled()).thenReturn(true);
					var sounds = mock(SoundPlayerService.class);
					var bundle = ResourceBundle.getBundle("i18n.messages", Locale.forLanguageTag(language));
					var controller = new SettingsChessController(settings, sounds, bundle);
					var loader = new FXMLLoader(getClass().getResource("/view/settings/settings_chess.fxml"), bundle);
					loader.setControllerFactory(_ -> controller);
					Parent root = loader.load();
					controller.onLoad(null);
					new Scene(root, 650, 500);
					root.applyCss();
					root.layout();
					var choices = (TilePane) root.lookup("#themes");
					assertEquals(6, choices.getChildren().size());
					assertTrue(((ToggleButton) choices.getChildren().getFirst()).isSelected());
					var preview = (GridPane) root.lookup("#preview");
					assertEquals(9, preview.getChildren().size());
					for (var button : choices.getChildren())
					{
						((ToggleButton) button).fire();
						var theme = (ChessBoardTheme) button.getUserData();
						assertTrue(preview.getChildren().getFirst().getStyle().contains(theme.light()));
					}
					((ToggleButton) choices.getChildren().get(1)).fire();
					root.applyCss();
					root.layout();
					if (language.equals("en"))
					{
						var imagePath = Path.of("build", "chess-settings-board.png");
						Files.createDirectories(imagePath.getParent());
						ImageIO.write(SwingFXUtils.fromFXImage(root.snapshot(null, null), null), "png", imagePath.toFile());
					}
					((TabPane) root.lookup(".tab-pane")).getSelectionModel().select(1);
					root.applyCss();
					root.layout();
					((CheckBox) root.lookup("#moveEnabled")).setSelected(false);
					((Button) root.lookup("#previewMove")).fire();
					((Button) root.lookup("#previewCapture")).fire();
					verify(sounds).previewChess(SoundPlayerService.SoundType.CHESS_MOVE);
					verify(sounds).previewChess(SoundPlayerService.SoundType.CHESS_CAPTURE);
					((Button) root.lookup("#previewDraw")).fire();
					verify(sounds).previewChess(SoundPlayerService.SoundType.CHESS_DRAW);
					((Button) root.lookup("#previewDefeat")).fire();
					verify(sounds).previewChess(SoundPlayerService.SoundType.CHESS_DEFEAT);
					((Button) root.lookup("#previewVictory")).fire();
					verify(sounds).previewChess(SoundPlayerService.SoundType.CHESS_VICTORY);
					controller.onSave();
					verify(settings).save(ChessBoardTheme.GREEN, false, true, false, false, false);
					if (language.equals("en"))
					{
						ImageIO.write(SwingFXUtils.fromFXImage(root.snapshot(null, null), null), "png",
								Path.of("build", "chess-settings-sound.png").toFile());
					}
				}
				done.complete(null);
			}
			catch (Throwable failure)
			{
				done.completeExceptionally(failure);
			}
		});
		done.get(30, TimeUnit.SECONDS);
	}
}