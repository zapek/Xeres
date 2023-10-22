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

import io.xeres.ui.support.contentline.ContentCode;

import java.util.regex.Pattern;

class CodeDetector implements MarkdownDetector
{
	private static final Pattern CODE_PATTERN = Pattern.compile("(`.*`)");

	@Override
	public boolean isPossibly(String line)
	{
		return line.contains("`");
	}

	@Override
	public void process(Context context, String line)
	{
		MarkdownService.processPattern(CODE_PATTERN, context, line, (s, groupName) -> context.addContent(new ContentCode(s.substring(1, s.length() - 1).strip())));
	}
}
