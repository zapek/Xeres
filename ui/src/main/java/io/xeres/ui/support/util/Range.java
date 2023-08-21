/*
 * Copyright (c) 2023 by David Gerber - https://zapek.com
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

package io.xeres.ui.support.util;

import java.util.regex.Matcher;

/**
 * Helper class to handle ranges of text. Provide a matcher then you can find the start and end of the range but
 * also the surrounding parts.
 */
public class Range
{
	private final int start;
	private final int end;

	public Range(Matcher matcher)
	{
		start = matcher.start(1);
		end = matcher.end();
	}

	public Range(int start, int end)
	{
		this.start = start;
		this.end = end;
	}

	public boolean hasRange()
	{
		return end > start;
	}

	public Range outerRange(Range other)
	{
		if (other.start > start)
		{
			// other is after us
			return new Range(end, other.start);
		}
		else
		{
			// other is before us
			return new Range(other.end, start);
		}
	}

	public int start()
	{
		return start;
	}

	public int end()
	{
		return end;
	}
}
