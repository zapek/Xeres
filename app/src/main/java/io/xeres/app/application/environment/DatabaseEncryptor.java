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

import com.github.windpapi4j.InitializationFailedException;
import com.github.windpapi4j.WinAPICallFailedException;
import com.github.windpapi4j.WinDPAPI;
import io.xeres.app.crypto.pgp.PGP;
import io.xeres.app.crypto.pgp.PGP.Armor;
import io.xeres.app.service.ProfileService;
import io.xeres.common.i18n.I18nUtils;
import io.xeres.common.mui.MUI;
import io.xeres.common.util.ScrambledString;
import io.xeres.common.util.SecureRandomUtils;
import org.apache.commons.lang3.SystemUtils;
import org.bouncycastle.openpgp.PGPException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.util.Objects;

/**
 * This class is responsible for handling the encryption of the database.
 * It is not a spring bean because it has to be available early, before even
 * spring is set up.
 */
public final class DatabaseEncryptor
{
	private static final Logger log = LoggerFactory.getLogger(DatabaseEncryptor.class);

	private static final int DATABASE_PASSWORD_LENGTH = 32;
	private static final String DATABASE_PREFIX = "userdata";
	private static final String DATABASE_SUFFIX = ".mv.db";
	private static final String DATABASE_ENCRYPTOR_FILE = DATABASE_PREFIX + ".key";
	private static final String DATABASE_AUTOLOGIN_FILE = DATABASE_PREFIX + ".auto";

	private String dataDir;

	// XXX: those 2 can linger around for far longer than necessary. check if it's possible to clear them once they're not needed anymore
	private ScrambledString passphrase;
	private ScrambledString databasePassword;
	private boolean isEncrypted;
	private boolean needsNewPassphrase;

	private DatabaseEncryptor()
	{
	}

	private static class SingletonHelper
	{
		private static final DatabaseEncryptor INSTANCE = new DatabaseEncryptor();
	}

	public static DatabaseEncryptor getInstance()
	{
		return SingletonHelper.INSTANCE;
	}

	public void init(String dataDir)
	{
		this.dataDir = dataDir;
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
		isEncrypted = checkIfEncrypted();
	}

	/**
	 * Checks if the database is encrypted. If there's no database yet,
	 * it's considered as encrypted too (will be created encrypted).
	 *
	 * @return yes if encrypted, false if plain
	 */
	public boolean isEncrypted()
	{
		checkInitialization();
		return isEncrypted;
	}

	public void setPassphrase(ScrambledString passphrase)
	{
		checkInitialization();
		this.passphrase = passphrase;
	}

	public void enableAutoLogin()
	{
		checkInitialization();
		Objects.requireNonNull(passphrase, "Passphrase must not be null for autologin to work, set it first");
		if (SystemUtils.IS_OS_WINDOWS)
		{
			try
			{
				var winDPAPI = WinDPAPI.newInstance();

				var scrambledDatabasePassword = new ScrambledString(getDatabasePassword());
				var pass = scrambledDatabasePassword.getAsByteArrayToClear();
				var cipherText = winDPAPI.protectData(pass);
				ScrambledString.clear(pass);
				scrambledDatabasePassword.dispose();

				try (var out = Files.newOutputStream(Path.of(dataDir, DATABASE_AUTOLOGIN_FILE)))
				{
					out.write(cipherText);
				}
			}
			catch (InitializationFailedException e)
			{
				log.error("DPAPI initialization failed", e);
			}
			catch (WinAPICallFailedException e)
			{
				log.error("DPAPI protectData() failed", e);
			}
			catch (IOException | InvalidKeyException e)
			{
				log.error("Couldn't get database password", e);
			}
			catch (PGPException _)
			{
				var e = new IllegalArgumentException(I18nUtils.getBundle().getString("mui.wrong-password"));
				MUI.getInstance().showError(e);
				throw e;
			}
		}
	}

	public void disableAutoLogin()
	{
		checkInitialization();
		if (SystemUtils.IS_OS_WINDOWS)
		{
			var path = Path.of(dataDir, DATABASE_AUTOLOGIN_FILE);
			if (Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
			{
				try
				{
					Files.delete(path);
				}
				catch (IOException e)
				{
					throw new RuntimeException(e);
				}
			}
		}
	}

	public boolean readAutoLogin()
	{
		checkInitialization();
		var path = Path.of(dataDir, DATABASE_AUTOLOGIN_FILE);
		if (SystemUtils.IS_OS_WINDOWS && Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) && Files.isReadable(path))
		{
			try
			{
				var winDPAPI = WinDPAPI.newInstance();

				try (var in = Files.newInputStream(path))
				{
					databasePassword = new ScrambledString(winDPAPI.unprotectData(in.readAllBytes()));
					return true;
				}
			}
			catch (InitializationFailedException e)
			{
				log.error("DPAPI initialization failed", e);
			}
			catch (IOException e)
			{
				log.error("Couldn't read autologin DPAPI file", e);
			}
			catch (WinAPICallFailedException e)
			{
				log.error("DPAPI unprotectData() failed", e);
			}
		}
		return false;
	}

	public boolean hasAutoLoginFile()
	{
		checkInitialization();
		if (SystemUtils.IS_OS_WINDOWS)
		{
			var path = Path.of(dataDir, DATABASE_AUTOLOGIN_FILE);
			return Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) && Files.isReadable(path);
		}
		return false;
	}

	public boolean isAutoLoginSupported()
	{
		// TODO: Enable a way to supply the passphrase on demand. Right now autologin will only be available after logging in at least once
		return SystemUtils.IS_OS_WINDOWS && (passphrase != null || databasePassword != null);
	}

	/**
	 * Gets the password for the database.
	 *
	 * @return the database password
	 * @throws IOException                if an I/O error occurred
	 * @throws PGPException               if a PGP error occurred
	 * @throws InvalidKeyException        if the PGP key is invalid
	 * @throws FileAlreadyExistsException if the database key storage file already exists but wasn't detected before
	 */
	public char[] getDatabasePassword() throws IOException, PGPException, InvalidKeyException
	{
		checkInitialization();
		if (databasePassword != null)
		{
			return databasePassword.getAsCharArrayToClear();
		}

		var path = Path.of(dataDir, DATABASE_ENCRYPTOR_FILE);
		if (ProfileService.hasSecretProfileKey())
		{
			var secretKey = PGP.getPGPSecretKey(ProfileService.getSecretProfileKey());
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

	public void lockDatabasePassword(ScrambledString databasePassword) throws InvalidKeyException, IOException, PGPException
	{
		checkInitialization();
		var path = Path.of(dataDir, DATABASE_ENCRYPTOR_FILE);
		var secretKey = PGP.getPGPSecretKey(ProfileService.getSecretProfileKey());
		PGP.encrypt(secretKey.getPublicKey(), new ByteArrayInputStream(databasePassword.getAsByteArrayToClear()), Files.newOutputStream(path), Armor.NONE);
	}

	public void setNeedsNewPassphrase(boolean enabled)
	{
		needsNewPassphrase = enabled;
	}

	public boolean isNewPassphraseNeeded()
	{
		return needsNewPassphrase;
	}

	private void checkInitialization()
	{
		if (dataDir == null)
		{
			throw new IllegalStateException("init() method has not been called");
		}
	}

	private boolean checkIfEncrypted()
	{
		var filePath = Path.of(dataDir, DATABASE_PREFIX + DATABASE_SUFFIX);
		if (Files.isRegularFile(filePath, LinkOption.NOFOLLOW_LINKS) && Files.isReadable(filePath))
		{
			try (var reader = new BufferedReader(new FileReader(filePath.toFile())))
			{
				var header = reader.readLine();
				if (header.startsWith("H2encrypt"))
				{
					return true;
				}
			}
			catch (IOException e)
			{
				throw new IllegalStateException("Couldn't read database: " + e.getMessage());
			}
		}
		else
		{
			return true; // If the file isn't there, then we want encryption for its creation
		}
		return false;
	}
}
