/*
 * Copyright (c) 2023 by David Gerber - https://zapek.com
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

package io.xeres.app.service.backup;

import io.xeres.app.service.LocationService;
import io.xeres.app.service.ProfileService;
import io.xeres.app.service.SettingsService;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.cert.CertificateEncodingException;

@Service
public class BackupService
{
	private static final Logger log = LoggerFactory.getLogger(BackupService.class);

	private static final long BACKUP_MAX_SIZE = 1024 * 1024 * 100L; // 100 MB

	private final ProfileService profileService;
	private final LocationService locationService;
	private final SettingsService settingsService;

	public BackupService(ProfileService profileService, LocationService locationService, SettingsService settingsService)
	{
		this.profileService = profileService;
		this.locationService = locationService;
		this.settingsService = settingsService;
	}

	public byte[] backup() throws JAXBException, CertificateEncodingException
	{
		var out = new ByteArrayOutputStream();

		var export = new Export();
		var local = new Local();
		local.setProfile(new Profile(settingsService.getSecretProfileKey()));
		local.setLocation(new Location(locationService.findOwnLocation().orElseThrow().getLocationId(),
				settingsService.getLocationPrivateKeyData(),
				settingsService.getLocationPublicKeyData(),
				settingsService.getLocationCertificate().getEncoded(),
				settingsService.getLocalPort()));

		export.setProfiles(profileService.getAllDiscoverableProfiles());
		export.setLocal(local);

		JAXBContext context;
		context = JAXBContext.newInstance(Export.class);

		var marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

		marshaller.marshal(export, out);
		return out.toByteArray();
	}

	@Transactional
	public void restore(MultipartFile file) throws JAXBException, IOException
	{
		if (file == null)
		{
			throw new IllegalArgumentException("XML backup file is empty");
		}

		if (file.getSize() >= BACKUP_MAX_SIZE)
		{
			throw new IllegalArgumentException("XML backup size is bigger than " + BACKUP_MAX_SIZE + " bytes");
		}

		JAXBContext context;
		context = JAXBContext.newInstance(Export.class);

		var unmarshaller = context.createUnmarshaller();

		var export = (Export) unmarshaller.unmarshal(file.getInputStream());

		log.debug("Export is {}", export);

		// XXX: when restoring check all the fields that we didn't save. for example pgpFingerprint will have to be regenerated (from the public key)
	}
}
