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

package io.xeres.app.xrs.service.turtle.item;

import io.xeres.app.xrs.serialization.RsSerialized;
import io.xeres.app.xrs.serialization.TlvType;
import io.xeres.common.id.Sha1Sum;

public class TurtleFileInfo
{
	@RsSerialized
	private long fileSize;

	@RsSerialized
	private Sha1Sum fileHash;

	@RsSerialized(tlvType = TlvType.STR_NAME)
	private String fileName;

	public TurtleFileInfo()
	{
		// Needed
	}

	public TurtleFileInfo(long fileSize, Sha1Sum fileHash, String fileName)
	{
		this.fileSize = fileSize;
		this.fileHash = fileHash;
		this.fileName = fileName;
	}

	public long getFileSize()
	{
		return fileSize;
	}

	public Sha1Sum getFileHash()
	{
		return fileHash;
	}

	public String getFileName()
	{
		return fileName;
	}

	@Override
	public String toString()
	{
		return "TurtleFileInfo{" +
				"fileSize=" + fileSize +
				", fileHash=" + fileHash +
				", fileName='" + fileName + '\'' +
				'}';
	}
}
