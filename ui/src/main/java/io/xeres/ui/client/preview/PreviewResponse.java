/*
 * Copyright (c) 2026 by David Gerber - https://zapek.com
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

package io.xeres.ui.client.preview;

import org.apache.commons.lang3.StringUtils;

public record PreviewResponse(
		String title,
		String description,
		String site,
		String thumbnailUrl,
		int thumbnailWidth,
		int thumbnailHeight
)
{
	public static final PreviewResponse EMPTY = new PreviewResponse(null, null, null, null, 0, 0);

	public PreviewResponse
	{
		title = StringUtils.abbreviate(title, 128);
		description = StringUtils.abbreviate(description, 256);
		site = StringUtils.abbreviate(site, 32);
		thumbnailUrl = StringUtils.truncate(thumbnailUrl, 2048);
	}

	public boolean isEmpty()
	{
		return equals(EMPTY);
	}

	public boolean hasInfo()
	{
		return StringUtils.isNotBlank(title) || StringUtils.isNotBlank(description) || StringUtils.isNotBlank(site) || hasThumbnail();
	}

	public boolean hasThumbnail()
	{
		return StringUtils.isNotBlank(thumbnailUrl);
	}

	public boolean hasThumbnailDimensions()
	{
		return hasThumbnail() && thumbnailWidth > 0 && thumbnailHeight > 0;
	}
}
