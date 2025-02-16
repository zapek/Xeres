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

package io.xeres.ui.support.sound;

import io.micrometer.common.util.StringUtils;
import javafx.scene.media.AudioClip;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class SoundService
{
	private final SoundSettings soundSettings;

	public enum SoundType
	{
		MESSAGE,
		HIGHLIGHT,
		FRIEND,
		DOWNLOAD
	}

	public SoundService(SoundSettings soundSettings)
	{
		this.soundSettings = soundSettings;
	}

	public void play(SoundType soundType)
	{
		switch (soundType)
		{
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
		}
	}

	public void play(String file)
	{
		if (StringUtils.isEmpty(file))
		{
			return;
		}

		var path = Path.of(file);
		if (!Files.exists(path) && !path.isAbsolute())
		{
			// Try to find the file if currentDir is not what we expect.
			// This happens on Windows when auto starting
			var home = new ApplicationHome(getClass());
			path = Path.of(home.getDir().toString(), file);
		}

		if (Files.exists(path))
		{
			var player = new AudioClip("file:" + file.replace("\\", "/")); // URIs require a '/' for path and Windows uses '\' for path
			player.play();
		}
	}
}
