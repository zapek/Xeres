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

package io.xeres.app.util;

import io.xeres.app.application.environment.DataDirLocator;
import org.bouncycastle.openpgp.PGPSecretKey;

import java.io.*;
import java.nio.file.Path;

public final class ProfileFileUtils
{
	private static final String PROFILE_FILE = "profile.sec";

	private ProfileFileUtils()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static boolean hasSecretProfileKey()
	{
		var dataDir = DataDirLocator.getDataDir();
		if (dataDir == null)
		{
			return false; // For tests
		}
		return Path.of(dataDir, PROFILE_FILE).toFile().isFile();
	}

	public static byte[] getSecretProfileKey()
	{
		var filePath = Path.of(DataDirLocator.getDataDir(), PROFILE_FILE);
		try (InputStream in = new FileInputStream(filePath.toFile()))
		{
			return in.readAllBytes();
		}
		catch (IOException e)
		{
			throw new IllegalStateException(e);
		}
	}

	public static void saveSecretProfileKey(PGPSecretKey pgpSecretKey) throws IOException
	{
		var filePath = Path.of(DataDirLocator.getDataDir(), PROFILE_FILE);
		try (OutputStream out = new FileOutputStream(filePath.toFile()))
		{
			pgpSecretKey.encode(out);
		}
	}
}
