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

package io.xeres.app.xrs.service.filetransfer;

import io.xeres.app.util.OsUtils;
import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileLeecherTest
{
	@Test
	void FileCreator_Sparse_OK()
	{
		var tempDir = System.getProperty("java.io.tmpdir");
		System.out.println("Temp dir: " + tempDir);
		var file = Paths.get(tempDir, "sparsefile.tmp").toFile();
		var fileCreator = new FileLeecher(file, 16384);
		fileCreator.open();
		fileCreator.close();

		if (SystemUtils.IS_OS_WINDOWS)
		{
			assertEquals("This file is set as sparse\n", OsUtils.shellExecute("fsutil", "sparse", "queryflag", file.getAbsolutePath()));
		}
		else if (SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC)
		{
			var result = OsUtils.shellExecute("ls", "-lsk", file.getAbsolutePath());
			var s = result.split(" ");
			var storageSize = Integer.parseInt(s[0]) * 1024;
			var fileSize = Integer.parseInt(s[5]);

			assertTrue(storageSize < fileSize);
		}

		file.delete();
	}
}