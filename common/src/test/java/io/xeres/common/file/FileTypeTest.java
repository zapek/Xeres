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

import java.util.HashSet;
import java.util.Set;

import static io.xeres.common.file.FileType.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FileTypeTest
{
	@Test
	void GetTypeByExtension_MissingExtension_Success()
	{
		assertEquals(ANY, getTypeByExtension("foobar."));
	}

	@Test
	void GetTypeByExtension_NoExtension_Success()
	{
		assertEquals(ANY, getTypeByExtension("foobar"));
	}

	@Test
	void GetTypeByExtension_Variants_Success()
	{
		assertEquals(AUDIO, getTypeByExtension("foobar.aac"));
		assertEquals(AUDIO, getTypeByExtension("foobar.mp3"));
		assertEquals(ARCHIVE, getTypeByExtension("foobar.tar"));
		assertEquals(DOCUMENT, getTypeByExtension("foobar.doc"));
		assertEquals(PICTURE, getTypeByExtension("foobar.jpg"));
		assertEquals(PROGRAM, getTypeByExtension("foobar.exe"));
		assertEquals(VIDEO, getTypeByExtension("foobar.avi"));
		assertEquals(SUBTITLES, getTypeByExtension("foobar.srt"));
		assertEquals(COLLECTION, getTypeByExtension("foobar.rscollection"));
	}

	@Test
	void GetTypeByExtension_NotFound_Success()
	{
		assertEquals(ANY, getTypeByExtension("foobar.dtc"));
	}

	/**
	 * Makes sure that no extension is in more than one group.
	 */
	@Test
	void GetExtensions_NoCrossMatches()
	{
		Set<String> all = new HashSet<>();
		all.addAll(AUDIO.getExtensions());
		all.addAll(ARCHIVE.getExtensions());
		all.addAll(DOCUMENT.getExtensions());
		all.addAll(PICTURE.getExtensions());
		all.addAll(PROGRAM.getExtensions());
		all.addAll(VIDEO.getExtensions());
		all.addAll(SUBTITLES.getExtensions());
		all.addAll(COLLECTION.getExtensions());

		assertEquals(all.size(),
				AUDIO.getExtensions().size() +
						ARCHIVE.getExtensions().size() +
						DOCUMENT.getExtensions().size() +
						PICTURE.getExtensions().size() +
						PROGRAM.getExtensions().size() +
						VIDEO.getExtensions().size() +
						SUBTITLES.getExtensions().size() +
						COLLECTION.getExtensions().size(),
				"There's a file extension which is in more than one group");
	}
}