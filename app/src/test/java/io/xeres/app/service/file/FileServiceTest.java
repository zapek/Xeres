/*
 * Copyright (c) 2023-2024 by David Gerber - https://zapek.com
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

package io.xeres.app.service.file;

import io.xeres.app.service.notification.file.FileNotificationService;
import io.xeres.common.id.Id;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(SpringExtension.class)
class FileServiceTest
{
	@Mock
	private FileNotificationService fileNotificationService;

	@InjectMocks
	private FileService fileService;

	@BeforeAll
	public static void setErrorLogging()
	{
		LoggingSystem.get(ClassLoader.getSystemClassLoader()).setLogLevel("io.xeres", LogLevel.DEBUG);
	}

	@Test
	void FileService_HashFile_OK() throws URISyntaxException
	{
		var hash = fileService.calculateFileHash(Path.of(Objects.requireNonNull(getClass().getResource("/image/leguman.jpg")).toURI()));
		assertNotNull(hash);
		assertEquals("0f02355b1b1e9a22801dddd85ded59fe7301698d", Id.toString(hash.getBytes()));
	}

//	@Test
//	void FileService_ScanShare_OK() throws URISyntaxException
//	{
//		fileService.scanShare(Path.of(Objects.requireNonNull(getClass().getResource("/image")).toURI()));
//		// XXX: check the hashes once a proper notification is sent
//	}
}