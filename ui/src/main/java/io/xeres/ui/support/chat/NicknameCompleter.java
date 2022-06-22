/*
 * Copyright (c) 2019-2022 by David Gerber - https://zapek.com
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

package io.xeres.ui.support.chat;

import java.util.function.Consumer;

public class NicknameCompleter
{
	public interface UsernameFinder
	{
		String getUsername(String prefix, int index);
	}

	private UsernameFinder usernameFinder;
	private int completionIndex;
	private boolean atStart;
	private String prefix;
	private boolean hasContext;
	private String lastSuggestedNickname;

	public void setUsernameFinder(UsernameFinder usernameFinder)
	{
		this.usernameFinder = usernameFinder;
	}

	public void complete(String line, int caretPosition, Consumer<String> action)
	{
		if (usernameFinder == null)
		{
			return;
		}

		if (!hasContext)
		{
			if (!line.contains(" "))
			{
				atStart = true;
			}
			prefix = findPrefix(line, caretPosition, atStart);
			hasContext = true;
		}
		var suggestedNickname = usernameFinder.getUsername(prefix, completionIndex);
		if (suggestedNickname != null)
		{
			if (atStart)
			{
				action.accept(suggestedNickname + ": ");
			}
			else
			{
				action.accept((lastSuggestedNickname != null ? line.substring(0, line.length() - lastSuggestedNickname.length()) : line.substring(0, line.length() - prefix.length())) + suggestedNickname);
			}
			lastSuggestedNickname = suggestedNickname;
		}
		completionIndex++;
	}

	public void reset()
	{
		completionIndex = 0;
		hasContext = false;
		lastSuggestedNickname = null;
	}

	private static String findPrefix(String line, int caretPosition, boolean atStart)
	{
		var start = atStart ? 0 : (line.lastIndexOf(" ", caretPosition) + 1);
		return line.substring(start, caretPosition);
	}
}
