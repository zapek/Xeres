/*
 * Copyright (c) 2024-2026 by David Gerber - https://zapek.com
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

package io.xeres.ui.support.sound;

import io.xeres.common.util.OsUtils;
import javafx.scene.media.AudioClip;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class SoundPlayerService
{
	private final SoundSettings soundSettings;
	private final io.xeres.ui.support.chess.ChessSettings chessSettings;
	private final java.util.Map<SoundType, AudioClip> chessClips = new java.util.EnumMap<>(SoundType.class);

	public enum SoundType
	{
		MESSAGE,
		HIGHLIGHT,
		FRIEND,
		DOWNLOAD,
		RINGING,
		CHESS_INVITE,
		CHESS_MOVE,
		CHESS_CAPTURE,
		CHESS_DRAW,
		CHESS_DEFEAT,
		CHESS_VICTORY
	}

	public SoundPlayerService(SoundSettings soundSettings, io.xeres.ui.support.chess.ChessSettings chessSettings)
	{
		this.soundSettings = soundSettings;
		this.chessSettings = chessSettings;
	}

	public void play(SoundType soundType)
	{
		switch (soundType)
		{
			case CHESS_MOVE, CHESS_CAPTURE, CHESS_DRAW, CHESS_DEFEAT, CHESS_VICTORY ->
			{
				var enabled = switch (soundType)
				{
					case CHESS_MOVE -> chessSettings.isMoveEnabled();
					case CHESS_CAPTURE -> chessSettings.isCaptureEnabled();
					case CHESS_DRAW -> chessSettings.isDrawEnabled();
					case CHESS_DEFEAT -> chessSettings.isDefeatEnabled();
					case CHESS_VICTORY -> chessSettings.isVictoryEnabled();
					default -> false;
				};
				if (enabled)
				{
					playChess(soundType);
				}
			}
			case CHESS_INVITE ->
			{
				if (chessSettings.isInviteEnabled())
				{
					playChess(soundType);
				}
			}
			case MESSAGE ->
			{
				if (soundSettings.isMessageEnabled())
				{
					play(soundSettings.getMessageFile());
				}
			}
			case HIGHLIGHT ->
			{
				if (soundSettings.isHighlightEnabled())
				{
					play(soundSettings.getHighlightFile());
				}
			}
			case FRIEND ->
			{
				if (soundSettings.isFriendEnabled())
				{
					play(soundSettings.getFriendFile());
				}
			}
			case DOWNLOAD ->
			{
				if (soundSettings.isDownloadEnabled())
				{
					play(soundSettings.getDownloadFile());
				}
			}
			case RINGING ->
			{
				if (soundSettings.isRingingEnabled())
				{
					play(soundSettings.getRingingFile());
				}
			}
		}
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
			org.slf4j.LoggerFactory.getLogger(SoundPlayerService.class).warn("Cannot play chess sound", exception);
		}
	}

	public AudioClip playRepeated(SoundType soundType)
	{
		switch (soundType)
		{
			case RINGING ->
			{
				if (soundSettings.isRingingEnabled())
				{
					return play(soundSettings.getRingingFile(), true);
				}
			}
		}
		return null;
	}

	public void play(String file)
	{
		play(file, false);
	}

	private AudioClip play(String file, boolean repeat)
	{
		if (StringUtils.isEmpty(file))
		{
			return null;
		}

		var path = Path.of(file);
		if (!Files.exists(path) && !path.isAbsolute())
		{
			// Try to find the file if currentDir is not what we expect.
			// This happens on Windows when auto starting
			var home = OsUtils.getApplicationHome();
			path = Path.of(home.toString(), file);

			// At some point (Spring probably), the currentDir returned by ApplicationHome() was changed from
			// where the application was installed to 'app'. We have to use the next workaround
			// to detect default paths set in the config prior to that.
			if (!Files.exists(path))
			{
				if (file.startsWith("app/"))
				{
					file = file.substring("app/".length());
					path = Path.of(home.toString(), file);
				}
			}
		}

		if (Files.exists(path))
		{
			var clip = new AudioClip("file:" + path.toString().replace("\\", "/")); // URIs require a '/' for path and Windows uses '\' for path
			if (repeat)
			{
				clip.setCycleCount(AudioClip.INDEFINITE);
			}
			clip.play();
			return clip;
		}
		return null;
	}
}
