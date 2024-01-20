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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FileTypeTest
{
	@Test
	void FileType_GetByExtension_NoExtension_OK()
	{
		assertEquals(FileType.ANY, FileType.getTypeByExtension("foobar."));
	}

	@Test
	void FileType_GetByExtension_NoExtension2_OK()
	{
		assertEquals(FileType.ANY, FileType.getTypeByExtension("foobar"));
	}

	@Test
	void FileType_GetByExtension_Found_OK()
	{
		assertEquals(FileType.AUDIO, FileType.getTypeByExtension("foobar.aac"));
		assertEquals(FileType.AUDIO, FileType.getTypeByExtension("foobar.mp3"));
		assertEquals(FileType.ARCHIVE, FileType.getTypeByExtension("foobar.tar"));
		assertEquals(FileType.CDIMAGE, FileType.getTypeByExtension("foobar.bin"));
		assertEquals(FileType.DOCUMENT, FileType.getTypeByExtension("foobar.doc"));
		assertEquals(FileType.PICTURE, FileType.getTypeByExtension("foobar.jpg"));
		assertEquals(FileType.PROGRAM, FileType.getTypeByExtension("foobar.bat"));
		assertEquals(FileType.VIDEO, FileType.getTypeByExtension("foobar.avi"));
	}

	@Test
	void FileType_GetByExtension_NotFound_OK()
	{
		assertEquals(FileType.ANY, FileType.getTypeByExtension("foobar.dtc"));
	}
}