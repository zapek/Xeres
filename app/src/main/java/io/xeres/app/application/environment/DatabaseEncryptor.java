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

package io.xeres.app.application.environment;

import io.xeres.app.crypto.pgp.PGP;
import io.xeres.common.util.ScrambledString;
import io.xeres.common.util.SecureRandomUtils;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPSecretKey;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;

public final class DatabaseEncryptor
{
	private static final String DATABASE_ENCRYPTOR_FILE = "userdata.key";

	private static ScrambledString scrambledString;

	private DatabaseEncryptor()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static void init(String dataDir)
	{
		if (Files.notExists(Path.of(dataDir, DATABASE_ENCRYPTOR_FILE)))
		{
			var password = new char[32];
			SecureRandomUtils.nextPassword(password);
			scrambledString = new ScrambledString(password);
			ScrambledString.clear(password);
		}
	}

	public static char[] getPassword(String dataDir, PGPSecretKey secretKey, ScrambledString passphrase) throws IOException, PGPException, InvalidKeyException
	{
		if (scrambledString != null)
		{
			return scrambledString.getAsArrayToClear();
		}
		var out = new ByteArrayOutputStream();
		PGP.decrypt(secretKey, passphrase, Files.newInputStream(Path.of(dataDir, DATABASE_ENCRYPTOR_FILE)), out);
		return out.toString().toCharArray();
	}
}
