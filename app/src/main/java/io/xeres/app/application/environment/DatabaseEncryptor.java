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
import io.xeres.app.util.DevUtils;
import io.xeres.app.util.ProfileFileUtils;
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

	/**
	 * Gets the instance of DatabaseEncryptor.
	 *
	 * @return the instance
	 */
	public static DatabaseEncryptor getInstance()
	{
		return SingletonHelper.INSTANCE;
	}

	/**
	 * Initializes the DatabaseEncryptor. Needs to be called once before any other method.
	 *
	 * @param dataDir the directory containing the database
	 */
	public void init(String dataDir)
	{
		this.dataDir = dataDir;
		if (dataDir == null)
		{
			return; // For tests
		}
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
	 * it's considered as encrypted too (at it will be created encrypted).
	 * <p>
	 * This is mostly for migration purposes.
	 *
	 * @return yes if encrypted, false if plain
	 */
	public boolean isEncrypted()
	{
		checkInitialization();
		return isEncrypted;
	}

	/**
	 * Sets a passphrase. Has to be done before operations needing a passphrase.
	 *
	 * @param passphrase the passphrase to set, will be disposed upon completion. Do not dispose it yourself!
	 */
	public void setPassphrase(ScrambledString passphrase)
	{
		checkInitialization();
		this.passphrase = passphrase;
	}

	/**
	 * Enables Auto-Login. A passphrase must have been set first.
	 */
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

	/**
	 * Disables Auto-Login.
	 */
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

	/**
	 * Reads the Auto-Login file if present and initializes the database password.
	 *
	 * @return true if successful
	 */
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

	/**
	 * Checks if the Auto-Login file is present.
	 * @return true if present
	 */
	public boolean hasAutoLoginFile()
	{
		checkInitialization();
		if (dataDir == null) // Needed for tests
		{
			return false;
		}
		if (SystemUtils.IS_OS_WINDOWS)
		{
			var path = Path.of(dataDir, DATABASE_AUTOLOGIN_FILE);
			return Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) && Files.isReadable(path);
		}
		return false;
	}

	/**
	 * Checks if Auto-Login is supported on this system.
	 * @return true if supported
	 */
	@SuppressWarnings("SameReturnValue")
	public boolean isAutoLoginSupported()
	{
		return SystemUtils.IS_OS_WINDOWS;
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
		if (ProfileFileUtils.hasSecretProfileKey())
		{
			Objects.requireNonNull(passphrase, "Passphrase must not be null for getDatabasePassword() to work");
			var secretKey = PGP.getPGPSecretKey(ProfileFileUtils.getSecretProfileKey());
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
				var scrambledString = new ScrambledString(password);
				var asCharArrayToClear = scrambledString.getAsCharArrayToClear();
				scrambledString.dispose();
				return asCharArrayToClear;
			}
			finally
			{
				ScrambledString.clear(password);
			}
		}
	}

	/**
	 * Locks the database password by rewriting it as encrypted. Requires a PGP key to be present.
	 * @param databasePassword the database password
	 * @throws InvalidKeyException the PGP key is invalid or missing
	 * @throws IOException I/O error
	 * @throws PGPException problem with the PGP key
	 */
	public void lockDatabasePassword(ScrambledString databasePassword) throws InvalidKeyException, IOException, PGPException
	{
		checkInitialization();
		var path = Path.of(dataDir, DATABASE_ENCRYPTOR_FILE);
		var secretKey = PGP.getPGPSecretKey(ProfileFileUtils.getSecretProfileKey());
		PGP.encrypt(secretKey.getPublicKey(), new ByteArrayInputStream(databasePassword.getAsByteArrayToClear()), Files.newOutputStream(path), Armor.NONE);
	}

	/**
	 * Clears the cached credentials. Must be done once they're not needed
	 * anymore.
	 */
	public void clearCredentials()
	{
		if (passphrase != null)
		{
			passphrase.dispose();
			passphrase = null;
		}
		if (databasePassword != null)
		{
			databasePassword.dispose();
			databasePassword = null;
		}
	}

	/**
	 * Indicates that we need a new passphrase to be setup as soon as possible.
	 * <p>Needed for upgrades where the original key had no passphrase set.
	 * @param enabled true if needed
	 */
	public void setNeedsNewPassphrase(boolean enabled)
	{
		needsNewPassphrase = enabled;
	}

	/**
	 * Checks if a new passphrase is needed.
	 * <p>Used for upgrades where the original key had no passphrase set.
	 * @return true if needed
	 */
	public boolean isNewPassphraseNeeded()
	{
		return needsNewPassphrase;
	}

	private void checkInitialization()
	{
		if (dataDir == null && !DevUtils.isTesting())
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
