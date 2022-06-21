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

import io.xeres.common.dto.identity.IdentityConstants;

import java.util.function.Consumer;

public class NicknameCompleter
{
	public interface UsernameFinder
	{
		String getUsername(String prefix, int index);
	}

	private UsernameFinder usernameFinder;
	private int completionIndex;

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

		String suggestedNickname = null;
		if (line.length() == 0)
		{
			// empty line, insert the first nickname
			suggestedNickname = usernameFinder.getUsername("", completionIndex);
		}
		else
		{
			if (caretPosition <= IdentityConstants.NAME_LENGTH_MAX) // XXX: remove this and have <tab> completer anywhere
			{
				var separator = line.indexOf(":");
				if (separator == -1)
				{
					separator = line.length();
				}
				suggestedNickname = usernameFinder.getUsername(line.substring(0, separator), completionIndex);
			}
		}

		if (suggestedNickname != null)
		{
			action.accept(suggestedNickname + ": "); // XXX: if this is not at the beginning, complete later
		}
		completionIndex++;
	}

	public void reset()
	{
		completionIndex = 0;
	}
}
