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

package io.xeres.ui.support.markdown;

import io.xeres.ui.support.contentline.ContentHeader;
import io.xeres.ui.support.contentline.ContentText;

class HeaderDetector implements MarkdownDetector
{
	@Override
	public boolean isPossibly(String line)
	{
		return line.startsWith("#");
	}

	@Override
	public void process(Context context, String line)
	{
		char space = '#';
		int size;

		for (size = 0; size < line.length(); size++)
		{
			space = line.charAt(size);
			if (space != '#')
			{
				break;
			}
		}

		if (space != ' ')
		{
			// Not a space, this is not a header
			context.addContent(new ContentText(line));
			return;
		}

		if (size > 6)
		{
			size = 6;
		}
		context.addContent(new ContentHeader(line.substring(size).trim() + context.getLn(), size));
	}
}
