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

package io.xeres.testutils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public final class ResourceUtils
{
	private ResourceUtils()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static File getResourceAsFile(String resourcePath)
	{
		try (InputStream in = ResourceUtils.class.getClassLoader().getResourceAsStream(resourcePath))
		{
			if (in == null) return null;
			File tempFile = File.createTempFile("xeres_test_resource_", ".tmp");
			tempFile.deleteOnExit();
			try (FileOutputStream out = new FileOutputStream(tempFile))
			{
				byte[] buffer = new byte[1024];
				int bytesRead;
				while ((bytesRead = in.read(buffer)) != -1)
				{
					out.write(buffer, 0, bytesRead);
				}
			}
			return tempFile;
		}
		catch (IOException e)
		{
			throw new RuntimeException("Couldn't copy test resource: " + e.getMessage());
		}
	}
}
