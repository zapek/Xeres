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

import io.xeres.ui.support.contentline.ContentUri;

import java.util.regex.Pattern;

class UrlDetector implements MarkdownDetector
{
	private static final Pattern URL_PATTERN = Pattern.compile("\\b(?<u>(?:https?|ftps?)://[-A-Z0-9+&@#/%?=~_|!:,.;]*[-A-Z0-9+&@#/%=~_|])|(?<e>[0-9A-Z._+\\-=]+@[0-9a-z\\-]+\\.[a-z]{2,})", Pattern.CASE_INSENSITIVE);

	@Override
	public boolean isPossibly(String line)
	{
		return line.contains("http") || line.contains("ftp") || line.contains("@");
	}

	@Override
	public void process(Context context, String line)
	{
		MarkdownService.processPattern(URL_PATTERN, context, line,
				(s, groupName) -> context.addContent(new ContentUri(s)));
	}
}
