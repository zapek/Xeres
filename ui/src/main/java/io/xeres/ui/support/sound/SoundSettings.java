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

import io.xeres.ui.support.preference.PreferenceUtils;
import org.apache.commons.lang3.SystemUtils;
import org.springframework.stereotype.Service;

import static io.xeres.ui.support.preference.PreferenceUtils.SOUND;

@Service
public class SoundSettings
{
	private static final String ENABLE_MESSAGE = "EnableMessage";
	private static final String ENABLE_HIGHLIGHT = "EnableHighlight";
	private static final String ENABLE_FRIEND = "EnableFriend";
	private static final String ENABLE_DOWNLOAD = "EnableDownload";

	private static final String MESSAGE_FILE = "MessageFile";
	private static final String HIGHLIGHT_FILE = "HighlightFile";
	private static final String FRIEND_FILE = "FriendFile";
	private static final String DOWNLOAD_FILE = "DownloadFile";

	private boolean messageEnabled;
	private boolean highlightEnabled;
	private boolean friendEnabled;
	private boolean downloadEnabled;

	private String messageFile;
	private String highlightFile;
	private String friendFile;
	private String downloadFile;

	private boolean loaded;

	public SoundSettings()
	{
	}

	public boolean isMessageEnabled()
	{
		loadIfNeeded();
		return messageEnabled;
	}

	public void setMessageEnabled(boolean messageEnabled)
	{
		this.messageEnabled = messageEnabled;
	}

	public String getMessageFile()
	{
		return messageFile;
	}

	public void setMessageFile(String messageFile)
	{
		this.messageFile = messageFile;
	}

	public boolean isHighlightEnabled()
	{
		loadIfNeeded();
		return highlightEnabled;
	}

	public void setHighlightEnabled(boolean highlightEnabled)
	{
		this.highlightEnabled = highlightEnabled;
	}

	public String getHighlightFile()
	{
		return highlightFile;
	}

	public void setHighlightFile(String highlightFile)
	{
		this.highlightFile = highlightFile;
	}

	public boolean isFriendEnabled()
	{
		loadIfNeeded();
		return friendEnabled;
	}

	public void setFriendEnabled(boolean friendEnabled)
	{
		this.friendEnabled = friendEnabled;
	}

	public String getFriendFile()
	{
		return friendFile;
	}

	public void setFriendFile(String friendFile)
	{
		this.friendFile = friendFile;
	}

	public boolean isDownloadEnabled()
	{
		loadIfNeeded();
		return downloadEnabled;
	}

	public void setDownloadEnabled(boolean downloadEnabled)
	{
		this.downloadEnabled = downloadEnabled;
	}

	public String getDownloadFile()
	{
		return downloadFile;
	}

	public void setDownloadFile(String downloadFile)
	{
		this.downloadFile = downloadFile;
	}

	private void loadIfNeeded()
	{
		if (loaded)
		{
			return;
		}
		var node = PreferenceUtils.getPreferences().node(SOUND);
		messageEnabled = node.getBoolean(ENABLE_MESSAGE, false);
		highlightEnabled = node.getBoolean(ENABLE_HIGHLIGHT, false);
		friendEnabled = node.getBoolean(ENABLE_FRIEND, false);
		downloadEnabled = node.getBoolean(ENABLE_DOWNLOAD, false);

		var prefixPath = SystemUtils.IS_OS_LINUX ? "/opt/xeres/lib/" : "";

		messageFile = node.get(MESSAGE_FILE, prefixPath + "app/sounds/message-notification-190034.mp3");
		highlightFile = node.get(HIGHLIGHT_FILE, prefixPath + "app/sounds/notification-4-126507.mp3");
		friendFile = node.get(FRIEND_FILE, prefixPath + "app/sounds/notification-20-270145.mp3");
		downloadFile = node.get(DOWNLOAD_FILE, prefixPath + "app/sounds/achive-sound-132273.mp3");

		loaded = true;
	}

	public void save()
	{
		var node = PreferenceUtils.getPreferences().node(SOUND);
		node.putBoolean(ENABLE_MESSAGE, messageEnabled);
		node.putBoolean(ENABLE_HIGHLIGHT, highlightEnabled);
		node.putBoolean(ENABLE_FRIEND, friendEnabled);
		node.putBoolean(ENABLE_DOWNLOAD, downloadEnabled);

		node.put(MESSAGE_FILE, messageFile);
		node.put(HIGHLIGHT_FILE, highlightFile);
		node.put(FRIEND_FILE, friendFile);
		node.put(DOWNLOAD_FILE, downloadFile);
	}
}
