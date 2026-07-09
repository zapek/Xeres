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

package io.xeres.app.service.script;

import io.xeres.common.util.image.ImageUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

class JsMultipartFile implements MultipartFile
{
	private final String name;
	private final MediaType mediaType;
	private final byte[] bytes;
	private InputStream inputStream;

	public JsMultipartFile(String name, byte[] bytes)
	{
		this.name = name;
		this.mediaType = ImageUtils.getImageMimeType(bytes);
		this.bytes = bytes;
	}

	@Override
	public @NonNull String getName()
	{
		return name;
	}

	@Override
	public @Nullable String getOriginalFilename()
	{
		return name;
	}

	@Override
	public @Nullable String getContentType()
	{
		return mediaType.toString();
	}

	@Override
	public boolean isEmpty()
	{
		return ArrayUtils.isEmpty(bytes);
	}

	@Override
	public long getSize()
	{
		return ArrayUtils.getLength(bytes);
	}

	@Override
	public byte @NonNull [] getBytes() throws IOException
	{
		return bytes;
	}

	@Override
	public @NonNull InputStream getInputStream()
	{
		if (inputStream == null)
		{
			inputStream = new ByteArrayInputStream(bytes);
		}
		return inputStream;
	}

	@Override
	public void transferTo(@NonNull File dest) throws IllegalStateException
	{
		throw new UnsupportedOperationException("Not implemented");
	}
}
