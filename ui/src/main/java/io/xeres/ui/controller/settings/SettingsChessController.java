/*
 * Copyright (c) 2024-2025 by David Gerber - https://zapek.com
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

import io.xeres.ui.controller.chess.ChessPieceView;
import io.xeres.ui.model.settings.Settings;
import io.xeres.ui.support.chess.ChessBoardTheme;
import io.xeres.ui.support.chess.ChessSettings;
import io.xeres.ui.support.sound.SoundPlayerService;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.stereotype.Component;

import java.util.ResourceBundle;

@Component
@FxmlView("/view/settings/settings_chess.fxml")
public class SettingsChessController implements SettingsController
{
	@FXML private TilePane themes;
	@FXML private GridPane preview;
	@FXML private Label themeName;
	@FXML private CheckBox inviteEnabled;
	@FXML private Button previewInvite;
	@FXML private CheckBox moveEnabled;
	@FXML private CheckBox captureEnabled;
	@FXML private Button previewMove;
	@FXML private CheckBox drawEnabled;
	@FXML private Button previewDraw;
	@FXML private CheckBox defeatEnabled;
	@FXML private Button previewDefeat;
	@FXML private CheckBox victoryEnabled;
	@FXML private Button previewVictory;
	@FXML private Button previewCapture;
	private final ToggleGroup selection = new ToggleGroup();
	private final ChessSettings chessSettings;
	private final SoundPlayerService sounds;
	private final ResourceBundle bundle;

	public SettingsChessController(ChessSettings chessSettings, SoundPlayerService sounds, ResourceBundle bundle)
	{
		this.chessSettings = chessSettings;
		this.sounds = sounds;
		this.bundle = bundle;
	}

	@Override
	public void initialize()
	{
		selection.getToggles().clear();
		themes.getChildren().clear();
		for (var theme : ChessBoardTheme.values())
		{
			var swatch = new GridPane();
			for (int row = 0; row < 2; row++)
			{
				for (int col = 0; col < 2; col++)
				{
					swatch.add(new Rectangle(26, 26, Color.web((row + col) % 2 == 0 ? theme.light() : theme.dark())), col, row);
				}
			}
			var button = new ToggleButton(bundle.getString("chess.theme." + theme.name()), swatch);
			button.setContentDisplay(ContentDisplay.TOP);
			button.setPrefWidth(104);
			button.setUserData(theme);
			button.setToggleGroup(selection);
			themes.getChildren().add(button);
		}
		selection.selectedToggleProperty().addListener((_, oldValue, value) -> {
			if (value == null && oldValue != null) oldValue.setSelected(true);
			else if (value != null) showPreview((ChessBoardTheme) value.getUserData());
		});
		previewInvite.setOnAction(_ -> sounds.previewChess(SoundPlayerService.SoundType.CHESS_INVITE));
		previewDraw.setOnAction(_ -> sounds.previewChess(SoundPlayerService.SoundType.CHESS_DRAW));
		previewDefeat.setOnAction(_ -> sounds.previewChess(SoundPlayerService.SoundType.CHESS_DEFEAT));
		previewVictory.setOnAction(_ -> sounds.previewChess(SoundPlayerService.SoundType.CHESS_VICTORY));
		previewMove.setOnAction(_ -> sounds.previewChess(SoundPlayerService.SoundType.CHESS_MOVE));
		previewCapture.setOnAction(_ -> sounds.previewChess(SoundPlayerService.SoundType.CHESS_CAPTURE));
	}

	private void showPreview(ChessBoardTheme theme)
	{
		themeName.setText(bundle.getString("chess.theme." + theme.name()));
		preview.getChildren().clear();
		var pieces = "bqp...NKR";
		for (int row = 0; row < 3; row++)
		{
			for (int col = 0; col < 3; col++)
			{
				var square = new StackPane();
				square.setPrefSize(76, 76);
				square.setStyle("-fx-background-color: " + ((row + col) % 2 == 0 ? theme.light() : theme.dark()) + ";");
				var piece = pieces.charAt(row * 3 + col);
				if (piece != '.')
				{
					var view = new ChessPieceView(piece);
					view.setMaxSize(68, 68);
					square.getChildren().add(view);
				}
				if (col == 0)
				{
					var rank = new Label(Integer.toString(8 - row));
					rank.setStyle("-fx-text-fill: " + ((row + col) % 2 == 0 ? theme.dark() : theme.light()) + "; -fx-font-weight: bold;");
					StackPane.setAlignment(rank, Pos.TOP_LEFT);
					square.getChildren().add(rank);
				}
				preview.add(square, col, row);
			}
		}
	}

	@Override
	public void onLoad(Settings unused)
	{
		for (var toggle : selection.getToggles())
		{
			if (toggle.getUserData() == chessSettings.getTheme()) toggle.setSelected(true);
		}
		inviteEnabled.setSelected(chessSettings.isInviteEnabled());
		drawEnabled.setSelected(chessSettings.isDrawEnabled());
		defeatEnabled.setSelected(chessSettings.isDefeatEnabled());
		victoryEnabled.setSelected(chessSettings.isVictoryEnabled());
		moveEnabled.setSelected(chessSettings.isMoveEnabled());
		captureEnabled.setSelected(chessSettings.isCaptureEnabled());
	}

	@Override
	public Settings onSave()
	{
		var toggle = selection.getSelectedToggle();
		if (toggle != null)
		{
			chessSettings.save((ChessBoardTheme) toggle.getUserData(), moveEnabled.isSelected(), captureEnabled.isSelected(), drawEnabled.isSelected(), defeatEnabled.isSelected(), victoryEnabled.isSelected(), inviteEnabled.isSelected());
		}
		return null;
	}
}