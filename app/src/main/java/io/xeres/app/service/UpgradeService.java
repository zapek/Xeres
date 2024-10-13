/*
 * Copyright (c) 2024 by David Gerber - https://zapek.com
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

import io.xeres.app.configuration.DataDirConfiguration;
import io.xeres.app.database.model.file.File;
import io.xeres.app.database.model.share.Share;
import io.xeres.app.service.file.FileService;
import io.xeres.app.xrs.service.identity.IdentityRsService;
import io.xeres.common.pgp.Trust;
import io.xeres.common.util.SecureRandomUtils;
import org.bouncycastle.openpgp.PGPException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

@Service
public class UpgradeService
{
	private final DataDirConfiguration dataDirConfiguration;
	private final SettingsService settingsService;
	private final FileService fileService;
	private final IdentityRsService identityRsService;
	private final ProfileService profileService;

	public UpgradeService(DataDirConfiguration dataDirConfiguration, SettingsService settingsService, FileService fileService, IdentityRsService identityRsService, ProfileService profileService)
	{
		this.dataDirConfiguration = dataDirConfiguration;
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
		var version = 3; // Increment this number when needing to add new defaults

		// Don't do this stuff when running tests
		if (dataDirConfiguration.getDataDir() == null)
		{
			return;
		}

		if (!settingsService.hasIncomingDirectory())
		{
			var incomingDirectory = Path.of(dataDirConfiguration.getDataDir(), "Incoming");
			if (Files.notExists(incomingDirectory))
			{
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
			fileService.addShare(Share.createShare("Incoming", File.createFile(incomingDirectory), false, Trust.UNKNOWN));
		}

		if (settingsService.getVersion() < 1)
		{
			var password = new char[20];
			SecureRandomUtils.nextPassword(password);
			settingsService.setRemotePassword(String.valueOf(password));
			Arrays.fill(password, (char) 0);
		}

		if (settingsService.getVersion() < 2)
		{
			fileService.encryptAllHashes();
		}

		if (settingsService.getVersion() < 3)
		{
			try
			{
				identityRsService.fixOwnProfile();
			}
			catch (PGPException | IOException e)
			{
				throw new IllegalStateException("Couldn't fix own profile hash + signature: " + e.getMessage());
			}
			profileService.fixAllProfiles();
		}

		// [Add new defaults here]

		settingsService.setVersion(version);
	}
}
