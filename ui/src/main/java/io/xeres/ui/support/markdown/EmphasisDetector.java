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

import io.xeres.ui.support.contentline.ContentEmphasis;

import java.util.EnumSet;
import java.util.regex.Pattern;

class EmphasisDetector implements MarkdownDetector
{
	private static final Pattern BOLD_AND_ITALIC_PATTERN = Pattern.compile("(?<b1>\\*\\*[\\p{L}\\p{Z}\\p{N}\\p{Pd}\\p{Pc}\\p{S}]{1,256}\\*\\*)|(?<i1>\\*[\\p{L}\\p{Z}\\p{N}\\p{Pd}\\p{Pc}\\p{S}]{1,256}\\*)|\\b(?<b2>__[\\p{L}\\p{Z}\\p{N}\\p{Pd}\\p{Pc}\\p{S}]{1,256}__)|\\b(?<i2>_[\\p{L}\\p{Z}\\p{N}\\p{Pd}\\p{Pc}\\p{S}]{1,256}_)");

	@Override
	public boolean isPossibly(String line)
	{
		return line.contains("*") || line.contains("_");
	}

	@Override
	public void process(Context context, String line)
	{
		MarkdownService.processPattern(BOLD_AND_ITALIC_PATTERN, context, line,
				(s, groupName) -> context.addContent(new ContentEmphasis(s.substring(groupName.startsWith("b") ? 2 : 1, s.length() - (groupName.startsWith("b") ? 2 : 1)), EnumSet.of(groupName.startsWith("b") ? ContentEmphasis.Style.BOLD : ContentEmphasis.Style.ITALIC))));
	}
}
