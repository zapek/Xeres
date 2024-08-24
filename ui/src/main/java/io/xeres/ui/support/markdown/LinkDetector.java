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

import io.xeres.ui.support.uri.UriFactory;

import java.util.regex.Pattern;

class LinkDetector implements MarkdownDetector
{
	private static final Pattern LINK_PATTERN = Pattern.compile("\\[.{0,256}]\\(.{0,2048}\\)"); // Large URL

	@Override
	public boolean isPossibly(String line)
	{
		return line.contains("[");
	}

	@Override
	public void process(Context context, String line)
	{
		MarkdownService.processPattern(LINK_PATTERN, context, line,
				(s, groupName) -> context.addContent(UriFactory.createContent(getUrl(s), getDescription(s), context.getUriAction())));
	}

	private static String getUrl(String s)
	{
		var index = s.lastIndexOf("(");
		return s.substring(index + 1, s.length() - 1); // skip the ")" at the end
	}

	private static String getDescription(String s)
	{
		var index = s.indexOf("]");
		return s.substring(1, index);
	}
}
