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

package io.xeres.app.configuration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class DataDirConfigurationTest
{
	@Mock
	private Environment environment;

	@InjectMocks
	private DataDirConfiguration dataDirConfiguration;

	@Test
	void DataDirConfiguration_GetDataDir_DataSourceAlreadySet_OK()
	{
		when(environment.getProperty("spring.datasource.url")).thenReturn("something");

		var dataDir = dataDirConfiguration.getDataDir();

		assertNull(dataDir);
	}

	@Test
	void DataDirConfiguration_PortableFileLocation_OK() throws IOException
	{
		var created = false;
		var portable = Path.of("portable");

		when(environment.acceptsProfiles(Profiles.of("dev"))).thenReturn(false);


		if (Files.notExists(portable))
		{
			Files.createFile(portable);
			created = true;
		}

		var dataDir = dataDirConfiguration.getDataDir();

		if (created)
		{
			Files.delete(portable);
		}

		assertEquals(portable.resolveSibling("data").toAbsolutePath().toString(), dataDir);
	}

	@Test
	void DataDirConfiguration_NativeFileLocation_OK() throws IOException
	{
		var deleted = false;
		var portable = Path.of("portable");

		when(environment.acceptsProfiles(Profiles.of("dev"))).thenReturn(false);

		if (Files.exists(portable))
		{
			Files.delete(portable);
			deleted = true;
		}

		var dataDir = dataDirConfiguration.getDataDir();

		if (deleted)
		{
			Files.createFile(portable);
		}

		assertNotEquals(portable.resolveSibling("data").toAbsolutePath().toString(), dataDir);
	}
}
