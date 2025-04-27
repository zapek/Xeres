/*
 * Copyright (c) 2025 by David Gerber - https://zapek.com
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

import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

class FileUploadTest
{
	private static final int TEMP_FILE_SIZE = 256;

	private static File createTempFile(int size) throws IOException
	{
		var tempFile = Files.createTempFile("fileseeder", ".tmp").toFile();
		if (size > 0)
		{
			Files.write(tempFile.toPath(), RandomUtils.insecure().randomBytes(size));
		}
		return tempFile;
	}

	private static void deleteTempFile(File file) throws IOException
	{
		Files.deleteIfExists(file.toPath());
	}

	@Test
	void GetFileSize_NotInitialized() throws IOException
	{
		var tempFile = createTempFile(0);
		var fileSeeder = new FileUpload(tempFile);
		assertThrows(IllegalStateException.class, fileSeeder::getFileSize);
		deleteTempFile(tempFile);
	}

	@Test
	void GetFileSize_Success() throws IOException
	{
		var tempFile = createTempFile(TEMP_FILE_SIZE);
		var fileSeeder = new FileUpload(tempFile);
		fileSeeder.open();
		assertEquals(TEMP_FILE_SIZE, fileSeeder.getFileSize());
		fileSeeder.close();
		deleteTempFile(tempFile);
	}

	@Test
	void Write_Illegal() throws IOException
	{
		var tempFile = createTempFile(TEMP_FILE_SIZE);
		var fileSeeder = new FileUpload(tempFile);
		fileSeeder.open();
		assertThrows(IllegalArgumentException.class, () -> fileSeeder.write(0, new byte[]{1, 2, 3}));
		fileSeeder.close();
		deleteTempFile(tempFile);
	}

	@Test
	void Read_Success() throws IOException
	{
		var tempFile = createTempFile(TEMP_FILE_SIZE);
		var fileSeeder = new FileUpload(tempFile);
		fileSeeder.open();
		assertArrayEquals(Files.readAllBytes(tempFile.toPath()), fileSeeder.read(0, TEMP_FILE_SIZE));
		fileSeeder.close();
		deleteTempFile(tempFile);
	}

	@Test
	void GetCompressedChunkMap_Success() throws IOException
	{
		var tempFile = createTempFile(TEMP_FILE_SIZE);
		var fileSeeder = new FileUpload(tempFile);
		fileSeeder.open();
		assertTrue(fileSeeder.getChunkMap().get(0));
		fileSeeder.close();
		deleteTempFile(tempFile);
	}

	@Test
	void IsComplete_Success() throws IOException
	{
		var tempFile = createTempFile(0);
		var fileSeeder = new FileUpload(tempFile);
		fileSeeder.isComplete();
		assertTrue(fileSeeder.isComplete());
		deleteTempFile(tempFile);
	}
}
