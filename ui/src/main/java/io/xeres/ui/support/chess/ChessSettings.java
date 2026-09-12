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

package io.xeres.ui.support.chess;

import io.xeres.ui.support.preference.PreferenceUtils;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.springframework.stereotype.Service;

@Service
public class ChessSettings
{
	private final ObjectProperty<ChessBoardTheme> theme = new SimpleObjectProperty<>(ChessBoardTheme.BROWN);
	private boolean inviteEnabled = true;
	private boolean moveEnabled = true;
	private boolean captureEnabled = true;
	private boolean drawEnabled = true;
	private boolean defeatEnabled = true;
	private boolean victoryEnabled = true;
	private boolean loaded;

	private void load()
	{
		if (loaded) return;
		var preferences = PreferenceUtils.getPreferences().node("Chess");
		theme.set(ChessBoardTheme.fromPreference(preferences.get("BoardTheme", "BROWN")));
		inviteEnabled = preferences.getBoolean("InviteSound", true);
		moveEnabled = preferences.getBoolean("MoveSound", true);
		captureEnabled = preferences.getBoolean("CaptureSound", true);
		drawEnabled = preferences.getBoolean("DrawSound", true);
		defeatEnabled = preferences.getBoolean("DefeatSound", true);
		victoryEnabled = preferences.getBoolean("VictorySound", true);
		loaded = true;
	}

	public ReadOnlyObjectProperty<ChessBoardTheme> themeProperty()
	{
		load();
		return theme;
	}

	public ChessBoardTheme getTheme()
	{
		load();
		return theme.get();
	}

	public boolean isInviteEnabled()
	{
		load();
		return inviteEnabled;
	}

	public boolean isMoveEnabled()
	{
		load();
		return moveEnabled;
	}

	public boolean isCaptureEnabled()
	{
		load();
		return captureEnabled;
	}

	public boolean isDrawEnabled()
	{
		load();
		return drawEnabled;
	}

	public boolean isDefeatEnabled()
	{
		load();
		return defeatEnabled;
	}

	public boolean isVictoryEnabled()
	{
		load();
		return victoryEnabled;
	}

	public void save(ChessBoardTheme selected, boolean move, boolean capture, boolean draw, boolean defeat, boolean victory)
	{
		save(selected, move, capture, draw, defeat, victory, isInviteEnabled());
	}

	public void save(ChessBoardTheme selected, boolean move, boolean capture, boolean draw, boolean defeat, boolean victory, boolean invite)
	{
		var preferences = PreferenceUtils.getPreferences().node("Chess");
		preferences.put("BoardTheme", selected.name());
		preferences.putBoolean("InviteSound", invite);
		preferences.putBoolean("MoveSound", move);
		preferences.putBoolean("CaptureSound", capture);
		preferences.putBoolean("DrawSound", draw);
		preferences.putBoolean("DefeatSound", defeat);
		preferences.putBoolean("VictorySound", victory);
		inviteEnabled = invite;
		moveEnabled = move;
		captureEnabled = capture;
		drawEnabled = draw;
		defeatEnabled = defeat;
		victoryEnabled = victory;
		loaded = true;
		theme.set(selected);
	}
}