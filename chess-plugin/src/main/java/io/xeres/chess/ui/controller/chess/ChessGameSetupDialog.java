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

package io.xeres.chess.ui.controller.chess;

import io.xeres.chess.common.dto.chess.ChessTimeControl;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.Locale;
import java.util.ResourceBundle;

/// Game setup for an open game (lobby seek), like RetroChess' `ChessGameSetupDialog`:
/// an "Unlimited" tab, and a "Real time" tab with minutes per side, increment and presets.
public class ChessGameSetupDialog extends Dialog<ChessTimeControl>
{
	/// Same presets as RetroChess (minutes, increment).
	static final int[][] PRESETS = {{1, 0}, {2, 1}, {3, 0}, {3, 2}, {5, 0}, {5, 3}, {10, 0}, {10, 5}, {15, 10}, {30, 0}, {30, 20}};

	private final ResourceBundle bundle;
	private final Slider minutes = new Slider(1, ChessTimeControl.MAX_MINUTES, 10);
	private final Slider increment = new Slider(0, ChessTimeControl.MAX_INCREMENT, 0);
	private final Label minutesValue = valueLabel();
	private final Label incrementValue = valueLabel();
	private final Label category = new Label();
	private final ToggleGroup presets = new ToggleGroup();
	private final TabPane tabs = new TabPane();
	private final Tab realTimeTab;
	private boolean updating;

	public ChessGameSetupDialog(ResourceBundle bundle)
	{
		this.bundle = bundle;
		setTitle(bundle.getString("chess.setup.title"));

		var heading = new Label(bundle.getString("chess.setup.title"));
		heading.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
		heading.setMaxWidth(Double.MAX_VALUE);
		heading.setAlignment(Pos.CENTER);

		var unlimitedText = new Label(bundle.getString("chess.setup.unlimited-text"));
		unlimitedText.setStyle("-fx-font-size: 14px;");
		var unlimitedPane = new StackPane(unlimitedText);
		unlimitedPane.setMinHeight(180);
		var unlimitedTab = new Tab(bundle.getString("chess.time.unlimited"), unlimitedPane);

		for (var slider : new Slider[]{minutes, increment})
		{
			slider.setBlockIncrement(1);
			slider.setPrefWidth(110);
			slider.valueProperty().addListener((_, _, _) -> sliderChanged());
		}
		var plus = new Label("+");
		var sliders = new HBox(8, new Label(bundle.getString("chess.setup.minutes")), minutes, minutesValue, plus,
				incrementValue, increment, new Label(bundle.getString("chess.setup.increment")));
		sliders.setAlignment(Pos.CENTER);

		var grid = new GridPane(6, 6);
		for (var i = 0; i < PRESETS.length; i++)
		{
			var preset = PRESETS[i];
			var button = new ToggleButton(preset[0] + "+" + preset[1]);
			button.setToggleGroup(presets);
			button.setUserData(preset);
			button.setMaxWidth(Double.MAX_VALUE);
			GridPane.setHgrow(button, Priority.ALWAYS);
			button.setOnAction(_ -> select(preset[0], preset[1]));
			grid.add(button, i % 4, i / 4);
		}
		category.setMaxWidth(Double.MAX_VALUE);
		category.setAlignment(Pos.CENTER);
		category.getStyleClass().add("text-muted");

		var realTimePane = new VBox(10, sliders, grid, category);
		realTimePane.setPadding(new Insets(10));
		realTimeTab = new Tab(bundle.getString("chess.setup.real-time"), realTimePane);

		tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
		tabs.getTabs().addAll(unlimitedTab, realTimeTab);

		var content = new VBox(12, heading, tabs);
		content.setPadding(new Insets(4, 8, 4, 8));
		content.setPrefWidth(620);
		getDialogPane().setContent(content);

		var create = new ButtonType(bundle.getString("chess.setup.create"), ButtonBar.ButtonData.OK_DONE);
		getDialogPane().getButtonTypes().addAll(create, ButtonType.CANCEL);
		var createButton = (Button) getDialogPane().lookupButton(create);
		createButton.getStyleClass().add("success");
		createButton.setDefaultButton(true);

		setResultConverter(type -> type == create ? selectedTimeControl() : null);
		select(10, 0);
	}

	private static Label valueLabel()
	{
		var label = new Label();
		label.setMinWidth(34);
		label.setAlignment(Pos.CENTER);
		label.setStyle("-fx-background-color: -color-fg-default; -fx-text-fill: -color-bg-default; -fx-font-weight: bold; -fx-padding: 2 6 2 6; -fx-background-radius: 4;");
		return label;
	}

	private void select(int minutesValueToSet, int incrementValueToSet)
	{
		updating = true;
		minutes.setValue(minutesValueToSet);
		increment.setValue(incrementValueToSet);
		updating = false;
		sliderChanged();
	}

	private void sliderChanged()
	{
		if (updating)
		{
			return;
		}
		var timeControl = ChessTimeControl.of(minutesValue(), incrementValue());
		minutesValue.setText(String.valueOf(timeControl.minutes()));
		incrementValue.setText(String.valueOf(timeControl.increment()));
		category.setText(bundle.getString("chess.time.category." + timeControl.category().toLowerCase(Locale.ROOT)));
		// Highlight the matching preset, if any.
		Toggle match = null;
		for (var toggle : presets.getToggles())
		{
			var preset = (int[]) toggle.getUserData();
			if (preset[0] == timeControl.minutes() && preset[1] == timeControl.increment())
			{
				match = toggle;
			}
		}
		presets.selectToggle(match);
	}

	private int minutesValue()
	{
		return (int) Math.clamp(Math.round(minutes.getValue()), 1, ChessTimeControl.MAX_MINUTES);
	}

	private int incrementValue()
	{
		return (int) Math.clamp(Math.round(increment.getValue()), 0, ChessTimeControl.MAX_INCREMENT);
	}

	/// The time control of the selected tab.
	ChessTimeControl selectedTimeControl()
	{
		return tabs.getSelectionModel().getSelectedItem() == realTimeTab
				? ChessTimeControl.of(minutesValue(), incrementValue())
				: ChessTimeControl.UNLIMITED;
	}
}
