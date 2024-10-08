/*
 * Copyright (c) 2024 by David Gerber - https://zapek.com
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

package io.xeres.ui.model.share;

import io.xeres.common.pgp.Trust;

import java.time.Instant;

public class Share
{
	private long id;
	private String name;
	private String path;
	private boolean searchable;
	private Trust browsable;
	private Instant lastScanned;

	public long getId()
	{
		return id;
	}

	public void setId(long id)
	{
		this.id = id;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getPath()
	{
		return path;
	}

	public void setPath(String path)
	{
		this.path = path;
	}

	public boolean isSearchable()
	{
		return searchable;
	}

	public void setSearchable(boolean searchable)
	{
		this.searchable = searchable;
	}

	public Trust getBrowsable()
	{
		return browsable;
	}

	public void setBrowsable(Trust browsable)
	{
		this.browsable = browsable;
	}

	public Instant getLastScanned()
	{
		return lastScanned;
	}

	public void setLastScanned(Instant lastScanned)
	{
		this.lastScanned = lastScanned;
	}
}
