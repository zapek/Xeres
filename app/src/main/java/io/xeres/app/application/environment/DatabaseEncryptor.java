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
import io.xeres.app.crypto.pgp.PGP.Armor;
import io.xeres.app.service.ProfileService;
import io.xeres.common.util.ScrambledString;
import io.xeres.common.util.SecureRandomUtils;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPSecretKey;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;

public final class DatabaseEncryptor
{
	private static final int DATABASE_PASSWORD_LENGTH = 32;
	private static final String DATABASE_ENCRYPTOR_FILE = "userdata.key";

	private static ScrambledString passphrase;

	private DatabaseEncryptor()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static void init(String dataDir)
	{
		var path = Path.of(dataDir, DATABASE_ENCRYPTOR_FILE);
		if (Files.notExists(path))
		{
			var password = new char[DATABASE_PASSWORD_LENGTH];
			SecureRandomUtils.nextPassword(password);
			var initialPassword = new ScrambledString(password);
			try (var outputStream = Files.newOutputStream(path))
			{
				outputStream.write(initialPassword.getAsByteArrayToClear());
			}
			catch (IOException e)
			{
				throw new RuntimeException(e);
			}
			initialPassword.dispose();
			ScrambledString.clear(password);
		}
	}

	public static boolean hasPassword()
	{
		return true; // XXX: for now... can it be used to enable/disable the feature?
	}

	public static void setPassphrase(ScrambledString passphrase)
	{
		DatabaseEncryptor.passphrase = passphrase;
	}

	/**
	 * Gets the password for the database.
	 *
	 * @param dataDir the data directory
	 * @return the database password
	 * @throws IOException                if an I/O error occurred
	 * @throws PGPException               if a PGP error occurred
	 * @throws InvalidKeyException        if the PGP key is invalid
	 * @throws FileAlreadyExistsException if the database key storage file already exists but wasn't detected before
	 */
	public static char[] getDatabasePassword(String dataDir) throws IOException, PGPException, InvalidKeyException
	{
		var path = Path.of(dataDir, DATABASE_ENCRYPTOR_FILE);
		if (ProfileService.hasSecretProfileKey())
		{
			PGPSecretKey secretKey = PGP.getPGPSecretKey(ProfileService.getSecretProfileKey());
			var out = new ByteArrayOutputStream();
			PGP.decrypt(secretKey, passphrase, Files.newInputStream(path), out);
			return out.toString().toCharArray();
		}
		else
		{
			byte[] password = null;
			try (var inputStream = Files.newInputStream(path))
			{
				password = inputStream.readAllBytes();
				var scrambleString = new ScrambledString(password);
				return scrambleString.getAsCharArrayToClear();
			}
			finally
			{
				ScrambledString.clear(password);
			}
		}
	}

	public static void lockDatabasePassword(String dataDir, ScrambledString passphrase) throws InvalidKeyException, IOException, PGPException
	{
		var path = Path.of(dataDir, DATABASE_ENCRYPTOR_FILE);
		PGPSecretKey secretKey = PGP.getPGPSecretKey(ProfileService.getSecretProfileKey());
		PGP.encrypt(secretKey.getPublicKey(), new ByteArrayInputStream(passphrase.getAsByteArrayToClear()), Files.newOutputStream(path), Armor.NONE);
	}
}
