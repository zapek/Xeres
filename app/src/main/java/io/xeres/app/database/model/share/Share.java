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

package io.xeres.app.database.model.share;

import io.xeres.app.database.converter.TrustConverter;
import io.xeres.app.database.model.file.File;
import io.xeres.common.pgp.Trust;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
public class Share
{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "file_id", nullable = false)
	private File file;

	@NotNull
	@Size(min = 1, max = 64)
	private String name;

	private boolean searchable;

	@Convert(converter = TrustConverter.class)
	private Trust browsable = Trust.UNKNOWN;

	protected Share()
	{
	}

	public long getId()
	{
		return id;
	}

	public void setId(long id)
	{
		this.id = id;
	}

	public File getFile()
	{
		return file;
	}

	public void setFile(File file)
	{
		this.file = file;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
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

	@Override
	public String toString()
	{
		return "Share{" +
				"id=" + id +
				", file=" + file +
				", name='" + name + '\'' +
				", searchable=" + searchable +
				", browsable=" + browsable +
				'}';
	}
}
