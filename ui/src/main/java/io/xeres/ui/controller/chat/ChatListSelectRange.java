/*
 * Copyright (c) 2025 by David Gerber - https://zapek.com
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

package io.xeres.ui.controller.chat;

import javafx.scene.text.HitInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ChatListSelectRange
{
	private static final Logger log = LoggerFactory.getLogger(ChatListSelectRange.class);

	private int start;
	private int end;

	public ChatListSelectRange(HitInfo firstHit, HitInfo secondHit)
	{
		var compare = compare(firstHit, secondHit);

		if (compare < 0)
		{
			start = firstHit.getCharIndex();
			end = secondHit.isLeading() ? secondHit.getCharIndex() : secondHit.getCharIndex() + 1;
		}
		else if (compare > 0)
		{
			start = secondHit.getCharIndex();
			end = firstHit.isLeading() ? firstHit.getCharIndex() : firstHit.getCharIndex() + 1;
		}
		else
		{
			start = firstHit.getCharIndex();
			end = secondHit.getCharIndex();
		}
	}

	public int getStart()
	{
		return start;
	}

	public int getEnd()
	{
		return end;
	}

	private static int compare(HitInfo firstHit, HitInfo secondHit)
	{
		if (firstHit.getCharIndex() == secondHit.getCharIndex())
		{
			if (firstHit.isLeading() == secondHit.isLeading())
			{
				return 0;
			}
			else if (firstHit.isLeading())
			{
				return -1;
			}
			else
			{
				return 1;
			}
		}
		return firstHit.getCharIndex() - secondHit.getCharIndex();
	}
}
