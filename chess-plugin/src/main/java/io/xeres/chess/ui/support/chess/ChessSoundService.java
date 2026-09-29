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

import javafx.scene.media.AudioClip;
import org.springframework.stereotype.Service;

@Service
public class ChessSoundService
{
	public enum SoundType { CHESS_INVITE, CHESS_MOVE, CHESS_CAPTURE, CHESS_DRAW, CHESS_DEFEAT, CHESS_VICTORY }

	private final ChessSettings chessSettings;
	private final java.util.Map<SoundType, AudioClip> chessClips = new java.util.EnumMap<>(SoundType.class);

	public ChessSoundService(ChessSettings chessSettings)
	{
		this.chessSettings = chessSettings;
	}

	public void play(SoundType type)
	{
		var enabled = switch (type)
		{
			case CHESS_INVITE -> chessSettings.isInviteEnabled();
			case CHESS_MOVE -> chessSettings.isMoveEnabled();
			case CHESS_CAPTURE -> chessSettings.isCaptureEnabled();
			case CHESS_DRAW -> chessSettings.isDrawEnabled();
			case CHESS_DEFEAT -> chessSettings.isDefeatEnabled();
			case CHESS_VICTORY -> chessSettings.isVictoryEnabled();
		};
		if (enabled) playChess(type);
	}

	public void previewChess(SoundType type)
	{
		if (type.name().startsWith("CHESS_")) playChess(type);
	}

	private void playChess(SoundType type)
	{
		try
		{
			var clip = chessClips.computeIfAbsent(type, key -> {
				var resource = "/sounds/" + key.name().toLowerCase(java.util.Locale.ROOT).replace('_', '-') + ".mp3";
				return new AudioClip(java.util.Objects.requireNonNull(getClass().getResource(resource)).toExternalForm());
			});
			clip.play();
		}
		catch (RuntimeException exception)
		{
			org.slf4j.LoggerFactory.getLogger(ChessSoundService.class).warn("Cannot play chess sound", exception);
		}
	}

}
