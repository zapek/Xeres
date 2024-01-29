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

import io.xeres.app.database.converter.FileTypeConverter;
import io.xeres.common.file.FileType;
import io.xeres.common.id.Sha1Sum;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
public class File
{
	private static final Logger log = LoggerFactory.getLogger(File.class);

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_id")
	private File parent;

	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, mappedBy = "parent", orphanRemoval = true)
	private List<File> children = new ArrayList<>();

	@NotNull
	@Size(min = 1, max = 255)
	private String name;

	@Convert(converter = FileTypeConverter.class)
	private FileType type;

	@Embedded
	@AttributeOverride(name = "identifier", column = @Column(name = "hash"))
	private Sha1Sum hash;

	private Instant modified;

	public static File createDirectory(File parent, String name, Instant modified)
	{
		var file = new File();
		file.setParent(parent);
		file.setName(name);
		file.setType(FileType.DIRECTORY);
		file.setModified(modified);
		return file;
	}

	public static File createFile(File parent, String name, Instant modified)
	{
		var file = new File();
		file.setParent(parent);
		file.setName(name);
		file.setType(FileType.getTypeByExtension(name));
		file.setModified(modified);
		return file;
	}

	public static File createFile(Path path)
	{
		path = getCanonicalPath(path);
		File file = createFile(path.getRoot().toString(), null);
		file.setType(FileType.DIRECTORY);

		for (Path component : path)
		{
			file = createFile(component.getFileName().toString(), file);
			file.setType(FileType.DIRECTORY);
		}
		return file;
	}

	private static Path getCanonicalPath(Path path)
	{
		try
		{
			return Path.of(path.toFile().getCanonicalPath());
		}
		catch (IOException e)
		{
			log.error("Failed to get canonical path: {}, using absolute path instead", path);
			return path.toAbsolutePath();
		}
	}

	private static File createFile(String name, File parent)
	{
		var file = new File();
		file.setName(name);

		if (parent != null)
		{
			file.setParent(parent);
		}
		return file;
	}

	public long getId()
	{
		return id;
	}

	public void setId(long id)
	{
		this.id = id;
	}

	public boolean hasParent()
	{
		return parent != null;
	}

	public File getParent()
	{
		return parent;
	}

	public void setParent(File parent)
	{
		this.parent = parent;
		parent.getChildren().add(parent);
	}

	public List<File> getChildren()
	{
		return children;
	}

	public void setChildren(List<File> children)
	{
		this.children = children;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public FileType getType()
	{
		return type;
	}

	public void setType(FileType type)
	{
		this.type = type;
	}

	public Sha1Sum getHash()
	{
		return hash;
	}

	public void setHash(Sha1Sum hash)
	{
		this.hash = hash;
	}

	public Instant getModified()
	{
		return modified;
	}

	public void setModified(Instant modified)
	{
		this.modified = modified;
	}

	@Override
	public String toString()
	{
		return "File{" +
				"id=" + id +
				", parent=" + parent +
				", name='" + name + '\'' +
				", type=" + type +
				", hash=" + hash +
				", modified=" + modified +
				'}';
	}
}
