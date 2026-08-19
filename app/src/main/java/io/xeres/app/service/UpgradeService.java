/*
 * Copyright (c) 2024-2026 by David Gerber - https://zapek.com
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

package io.xeres.app.service;

import io.xeres.app.application.environment.DataDirLocator;
import io.xeres.app.application.environment.DatabaseEncryptor;
import io.xeres.app.database.model.file.File;
import io.xeres.app.database.model.share.Share;
import io.xeres.app.service.file.FileService;
import io.xeres.app.xrs.service.identity.IdentityRsService;
import io.xeres.common.pgp.Trust;
import io.xeres.common.util.ScrambledString;
import io.xeres.common.util.SecureRandomUtils;
import org.bouncycastle.openpgp.PGPException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.util.Arrays;

@Service
public class UpgradeService
{
	private static final Logger log = LoggerFactory.getLogger(UpgradeService.class);

	private static final String INCOMING_DIRECTORY_NAME = "Incoming";
	private static final String STICKERS_DIRECTORY_NAME = "Stickers";

	private final SettingsService settingsService;
	private final FileService fileService;
	private final IdentityRsService identityRsService;
	private final ProfileService profileService;

	public UpgradeService(SettingsService settingsService, FileService fileService, IdentityRsService identityRsService, ProfileService profileService)
	{
		this.settingsService = settingsService;
		this.fileService = fileService;
		this.identityRsService = identityRsService;
		this.profileService = profileService;
	}

	/**
	 * Configures defaults and upgrades that cannot be done on the database definition alone because
	 * they depend on some runtime parameters. This is not called in UI client only mode.
	 */
	public void upgrade()
	{
		var version = 6; // Increment this number when needing to add new defaults

		// Don't do this stuff when running tests
		if (DataDirLocator.getDataDir() == null)
		{
			return;
		}

		if (!settingsService.hasIncomingDirectory())
		{
			log.debug("Checking for incoming directory...");
			var incomingDirectory = Path.of(DataDirLocator.getDataDir(), INCOMING_DIRECTORY_NAME);
			if (Files.notExists(incomingDirectory))
			{
				log.debug("Creating incoming directory...");
				try
				{
					Files.createDirectory(incomingDirectory);
				}
				catch (IOException e)
				{
					throw new IllegalStateException("Couldn't create incoming directory: " + incomingDirectory + ", :" + e.getMessage());
				}
			}
			settingsService.setIncomingDirectory(incomingDirectory.toString());
			fileService.addShare(Share.createShare(INCOMING_DIRECTORY_NAME, File.createFile(incomingDirectory), false, Trust.UNKNOWN));
		}

		if (settingsService.getVersion() < 1)
		{
			log.debug("Setting up remote password in database...");
			var password = new char[20];
			SecureRandomUtils.nextPassword(password);
			settingsService.setRemotePassword(String.valueOf(password));
			Arrays.fill(password, (char) 0);

			// Version 1 was done long ago, but we perform it here
			// to make sure vanilla installations have DHT turned off.
			settingsService.setDhtEnabled(false);
		}

		if (settingsService.getVersion() < 2)
		{
			log.debug("Encrypting all hashes...");
			fileService.encryptAllHashes();
		}

		if (settingsService.getVersion() < 4)
		{
			log.debug("Checking for stickers...");
			var stickersDirectory = Path.of(DataDirLocator.getDataDir(), STICKERS_DIRECTORY_NAME);
			if (Files.notExists(stickersDirectory))
			{
				log.debug("Creating stickers directory...");
				try
				{
					Files.createDirectory(stickersDirectory);
				}
				catch (IOException e)
				{
					// Not very important, we can live without stickers.
					log.error("Couldn't create stickers directory: {}, {}. Stickers won't be available", stickersDirectory, e.getMessage());
				}
			}
		}

		if (settingsService.getVersion() < 5)
		{
			log.debug("Checking for own identity fix...");
			// Removing the service string will change the identity's signature,
			// so we need to recompute it again.
			identityRsService.fixOwnIdentity();
		}

		// Encryption at rest.
		// Move the private key into its own file and remove it from the database
		if (settingsService.getVersion() < 6)
		{
			log.debug("Setting up encryption at rest...");
			//noinspection deprecation
			var secretProfileKeyData = settingsService.getSecretProfileKey();
			if (secretProfileKeyData != null)
			{
				log.info("Migrating secret key from database to file");
				try
				{
					var databaseEncryptor = DatabaseEncryptor.getInstance();
					var databasePassword = new ScrambledString(databaseEncryptor.getDatabasePassword());
					profileService.transferSecretProfileKeyData(secretProfileKeyData);
					//noinspection deprecation
					settingsService.saveSecretProfileKey(null); // Clear it, it's migrated
					databaseEncryptor.setPassphrase(new ScrambledString("")); // This is the password that was used for the key without encryption
					databaseEncryptor.setNeedsNewPassphrase(true); // So we ask the user to set a new one
					databaseEncryptor.lockDatabasePassword(databasePassword);
					databasePassword.dispose();
				}
				catch (PGPException | InvalidKeyException | IOException e)
				{
					throw new IllegalStateException("Couldn't transfer private key", e);
				}
			}
		}

		// This "old" fix needs to be done after encryption at rest because
		// it requires the profile in the new location to sign again.
		if (settingsService.getVersion() < 3)
		{
			log.debug("Fixing own profile...");
			try
			{
				identityRsService.fixOwnProfile();
			}
			catch (PGPException | InvalidKeyException | IOException e)
			{
				throw new IllegalStateException("Couldn't fix own profile hash + signature: " + e.getMessage());
			}
			log.debug("Fixing all profile...");
			profileService.fixAllProfiles();
		}

		// [Add new defaults here]

		settingsService.setVersion(version);
	}
}
