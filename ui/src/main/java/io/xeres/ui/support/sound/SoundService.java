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
import io.xeres.common.util.OsUtils;
import javafx.scene.media.AudioClip;
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
		DOWNLOAD,
		RINGING
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
			case RINGING ->
			{
				if (soundSettings.isRingingEnabled())
				{
					play(soundSettings.getRingingFile());
				}
			}
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
