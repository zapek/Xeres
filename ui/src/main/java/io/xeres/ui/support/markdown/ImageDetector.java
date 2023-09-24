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

import io.xeres.ui.support.contentline.ContentImage;
import io.xeres.ui.support.contentline.ContentText;
import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

class ImageDetector implements MarkdownDetector
{
	private static final Logger log = LoggerFactory.getLogger(ImageDetector.class);

	private static final Pattern IMAGE_PATTERN = Pattern.compile("!\\[.{0,256}]\\(.{0,264670}\\)"); // Maximum size of a gxs message + 30% of base 64 encoding

	@Override
	public boolean isPossibly(String line)
	{
		return line.contains("![");
	}

	@Override
	public void process(Context context, String line)
	{
		MarkdownService.processPattern(IMAGE_PATTERN, context, line,
				(s, groupName) -> {
					var image = getImage(s);
					if (image != null)
					{
						context.addContent(new ContentImage(image));
					}
					else
					{
						context.addContent(new ContentText("[image corrupted]"));
					}
				});
	}

	private static Image getImage(String s)
	{
		Image image = null;
		var index = s.indexOf("](data:");
		var data = s.substring(index + 2, s.length() - 1); // skip "](" and the ")" at the end
		try
		{
			image = new Image(data);
			if (image.isError())
			{
				image = null;
			}
		}
		catch (IllegalArgumentException e)
		{
			log.error("Error while loading image", e);
		}
		return image;
	}
}
