/*
 * Copyright (c) 2019-2022 by David Gerber - https://zapek.com
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

package io.xeres.app.xrs.service.turtle;

import io.xeres.app.database.model.location.Location;

import java.time.Instant;

class SearchRequest
{
	private final Location source;
	private final Instant lastUsed;
	private final int depth;
	private final String keywords;
	private final int resultCount;
	private final int hitLimit;

	public SearchRequest(Location source, int depth, String keywords, int resultCount, int hitLimit)
	{
		this.source = source;
		this.lastUsed = Instant.now();
		this.depth = depth;
		this.keywords = keywords;
		this.resultCount = resultCount;
		this.hitLimit = hitLimit;
	}

	public Location getSource()
	{
		return source;
	}

	public Instant getLastUsed()
	{
		return lastUsed;
	}

	public int getDepth()
	{
		return depth;
	}

	public String getKeywords()
	{
		return keywords;
	}

	public int getResultCount()
	{
		return resultCount;
	}

	public int getHitLimit()
	{
		return hitLimit;
	}
}
