package io.xeres.app.xrs.service.filetransfer;

import io.xeres.testutils.RandomUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

class FileSeederTest
{
	private static final int TEMP_FILE_SIZE = 256;

	private static File createTempFile(int size) throws IOException
	{
		var tempFile = Files.createTempFile("fileseeder", ".tmp").toFile();
		if (size > 0)
		{
			Files.write(tempFile.toPath(), RandomUtils.nextBytes(size));
		}
		return tempFile;
	}

	private static void deleteTempFile(File file) throws IOException
	{
		Files.deleteIfExists(file.toPath());
	}

	@Test
	void FileSeeder_GetFileSize_NotInitialized() throws IOException
	{
		var tempFile = createTempFile(0);
		var fileSeeder = new FileSeeder(tempFile);
		assertThrows(IllegalStateException.class, fileSeeder::getFileSize);
		deleteTempFile(tempFile);
	}

	@Test
	void FileSeeder_GetFileSize_OK() throws IOException
	{
		var tempFile = createTempFile(TEMP_FILE_SIZE);
		var fileSeeder = new FileSeeder(tempFile);
		fileSeeder.open();
		assertEquals(TEMP_FILE_SIZE, fileSeeder.getFileSize());
		fileSeeder.close();
		deleteTempFile(tempFile);
	}

	@Test
	void FileSeeder_Write_Illegal() throws IOException
	{
		var tempFile = createTempFile(TEMP_FILE_SIZE);
		var fileSeeder = new FileSeeder(tempFile);
		fileSeeder.open();
		assertThrows(IllegalArgumentException.class, () -> fileSeeder.write(0, new byte[]{1, 2, 3}));
		fileSeeder.close();
		deleteTempFile(tempFile);
	}

	@Test
	void FileSeeder_Read_OK() throws IOException
	{
		var tempFile = createTempFile(TEMP_FILE_SIZE);
		var fileSeeder = new FileSeeder(tempFile);
		fileSeeder.open();
		assertArrayEquals(Files.readAllBytes(tempFile.toPath()), fileSeeder.read(0, TEMP_FILE_SIZE));
		fileSeeder.close();
		deleteTempFile(tempFile);
	}

	@Test
	void FileSeeder_GetCompressedChunkMap_OK() throws IOException
	{
		var tempFile = createTempFile(TEMP_FILE_SIZE);
		var fileSeeder = new FileSeeder(tempFile);
		fileSeeder.open();
		assertTrue(fileSeeder.getChunkMap().get(0));
		fileSeeder.close();
		deleteTempFile(tempFile);
	}

	@Test
	void FileSeeder_IsComplete_OK() throws IOException
	{
		var tempFile = createTempFile(0);
		var fileSeeder = new FileSeeder(tempFile);
		fileSeeder.isComplete();
		assertTrue(fileSeeder.isComplete());
		deleteTempFile(tempFile);
	}
}
