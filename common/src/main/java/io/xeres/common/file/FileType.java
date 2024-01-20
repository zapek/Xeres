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

package io.xeres.common.file;

import io.xeres.common.i18n.I18nEnum;
import io.xeres.common.i18n.I18nUtils;

import java.util.List;

public enum FileType implements I18nEnum
{
	ANY(List.of()),
	AUDIO(List.of("aac", "aif", "flac", "iff", "m3u", "m4a", "mid", "midi", "mp3", "mpa", "ogg", "ra", "ram", "wav", "wma", "weba")),
	ARCHIVE(List.of("7z", "bz2", "gz", "pkg", "rar", "sea", "sit", "sitx", "tar", "zip", "tgz")),
	CDIMAGE(List.of("iso", "nrg", "mdf", "bin")),
	DOCUMENT(List.of("doc", "odt", "ott", "rtf", "pdf", "ps", "txt", "log", "msg", "wpd", "wps", "ods", "xls", "epub")),
	PICTURE(List.of("3dm", "3dmf", "ai", "bmp", "drw", "dxf", "eps", "gif", "ico", "indd", "jpe", "jpeg", "jpg", "mng", "pcx", "pcc", "pct", "pgm", "pix", "png", "psd", "qxd", "qxp", "rgb", "sgi", "svg", "tga", "tif", "tiff", "xbm", "xcf", "webp")),
	PROGRAM(List.of("app", "bat", "cgi", "com", "exe", "js", "pif", "py", "pl", "sh", "vb", "ws", "bash")), // XXX: RS has "bin" here too
	VIDEO(List.of("3gp", "asf", "asx", "avi", "mov", "mp4", "mkv", "flv", "mpeg", "mpg", "qt", "rm", "swf", "vob", "wmv", "webm")),
	DIRECTORY(List.of());

	private final List<String> extensions;

	FileType(List<String> extensions)
	{
		this.extensions = extensions;
	}

	public List<String> getExtensions()
	{
		return extensions;
	}

	@Override
	public String toString()
	{
		return I18nUtils.getString(getMessageKey(this));
	}

	public static FileType getTypeByExtension(String filename)
	{
		var index = filename.lastIndexOf(".");
		if (index == -1)
		{
			return ANY;
		}
		var extension = filename.substring(index + 1);
		if (extension.isEmpty())
		{
			return ANY;
		}
		for (var value : values())
		{
			if (value.getExtensions().contains(extension))
			{
				return value;
			}
		}
		return ANY;
	}
}
