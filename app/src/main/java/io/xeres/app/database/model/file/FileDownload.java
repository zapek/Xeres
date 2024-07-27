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

package io.xeres.app.database.model.file;

import io.xeres.common.id.Sha1Sum;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.BitSet;

@Entity
public class FileDownload
{
	private static final int NAME_SIZE_MIN = 1;
	private static final int NAME_SIZE_MAX = 255;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@NotNull
	@Size(min = NAME_SIZE_MIN, max = NAME_SIZE_MAX)
	private String name;

	private long size;

	@Embedded
	@AttributeOverride(name = "identifier", column = @Column(name = "hash"))
	private Sha1Sum hash;

	private BitSet chunkMap = new BitSet();

	private boolean completed;

	public long getId()
	{
		return id;
	}

	public void setId(long id)
	{
		this.id = id;
	}

	public @NotNull @Size(min = NAME_SIZE_MIN, max = NAME_SIZE_MAX) String getName()
	{
		return name;
	}

	public void setName(@NotNull @Size(min = NAME_SIZE_MIN, max = NAME_SIZE_MAX) String name)
	{
		this.name = name;
	}

	public long getSize()
	{
		return size;
	}

	public void setSize(long size)
	{
		this.size = size;
	}

	public Sha1Sum getHash()
	{
		return hash;
	}

	public void setHash(Sha1Sum hash)
	{
		this.hash = hash;
	}

	public BitSet getChunkMap()
	{
		return chunkMap;
	}

	public void setChunkMap(BitSet chunkMap)
	{
		this.chunkMap = chunkMap;
	}

	public boolean isCompleted()
	{
		return completed;
	}

	public void setCompleted(boolean completed)
	{
		this.completed = completed;
	}

	@Override
	public String toString()
	{
		return "FileDownload{" +
				"id=" + id +
				", name='" + name + '\'' +
				", size=" + size +
				", hash=" + hash +
				", chunkMap=" + chunkMap +
				", completed=" + completed +
				'}';
	}
}
