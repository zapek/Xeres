/*
 * Copyright (c) 2023-2025 by David Gerber - https://zapek.com
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

import io.xeres.ui.support.contentline.ContentEmoji;

import java.util.regex.Pattern;

class EmojiDetector implements MarkdownDetector
{
	private static final Pattern EMOJI_PATTERN = Pattern.compile("(" +
			"[\\x{1F1E6}-\\x{1F1FF}]{2}" + // Regional Indicator
			"|(\\p{IsEmoji}" + // Emoji Sequence
			"(\\p{IsEmoji_Modifier}" +
			"|\\x{FE0F}\\x{20E3}?" + // Emoji Presentation Sequence (with optional Keycap)
			"|[\\x{E0020}-\\x{E007E}]+\\x{E007F}" + // Emoji Tag Sequence
			")" +
			"|\\p{IsEmoji_Presentation}" + // Single Character Emoji
			")" +
			"(\\x{200D}" + // Emoji Zero-Width Joiner Sequence (ZWJ)
			"(\\p{IsEmoji}" +
			"(\\p{IsEmoji_Modifier}" +
			"|\\x{FE0F}\\x{20E3}?" +
			"|[\\x{E0020}-\\x{E007E}]+\\x{E007F}" +
			")" +
			"|\\p{IsEmoji_Presentation}" +
			")" +
			"){0,256}" +
			")");

	@Override
	public boolean isPossibly(String line)
	{
		return !line.chars().allMatch(c -> c < 128); // Detects non-ASCII
	}

	@Override
	public void process(Context context, String line)
	{
		MarkdownService.processPattern(EMOJI_PATTERN, context, line,
				(s, groupName) -> context.addContent(new ContentEmoji(context.getEmojiService().getEmoji(s), s)));
	}
}
