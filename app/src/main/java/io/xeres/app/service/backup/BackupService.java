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

import io.xeres.app.crypto.pgp.PGP;
import io.xeres.app.crypto.rsa.RSA;
import io.xeres.app.service.IdentityService;
import io.xeres.app.service.LocationService;
import io.xeres.app.service.ProfileService;
import io.xeres.app.service.SettingsService;
import io.xeres.app.xrs.service.identity.IdentityRsService;
import io.xeres.common.id.ProfileFingerprint;
import io.xeres.common.pgp.Trust;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPKeyPair;
import org.bouncycastle.openpgp.jcajce.JcaPGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPDigestCalculatorProviderBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;

@Service
public class BackupService
{
	private static final Logger log = LoggerFactory.getLogger(BackupService.class);

	private static final long BACKUP_MAX_SIZE = 1024 * 1024 * 100L; // 100 MB

	private final ProfileService profileService;
	private final LocationService locationService;
	private final IdentityService identityService;
	private final IdentityRsService identityRsService;
	private final SettingsService settingsService;

	public BackupService(ProfileService profileService, LocationService locationService, IdentityService identityService, IdentityRsService identityRsService, SettingsService settingsService)
	{
		this.profileService = profileService;
		this.locationService = locationService;
		this.identityService = identityService;
		this.identityRsService = identityRsService;
		this.settingsService = settingsService;
	}

	public byte[] backup() throws JAXBException
	{
		var out = new ByteArrayOutputStream();

		var export = new Export();
		var local = new Local();
		local.setProfile(new Profile(settingsService.getSecretProfileKey()));
		local.setLocation(new Location(locationService.findOwnLocation().orElseThrow().getLocationId(),
				settingsService.getLocationPrivateKeyData(),
				settingsService.getLocationPublicKeyData(),
				settingsService.getLocationCertificate(),
				settingsService.getLocalPort()));

		var identityGroupItem = identityService.getOwnIdentity();
		local.setIdentity(new Identity(identityGroupItem.getName(), identityGroupItem.getAdminPrivateKey().getEncoded(), identityGroupItem.getAdminPublicKey().getEncoded()));

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
	public void restore(MultipartFile file) throws JAXBException, IOException, InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException, CertificateException, PGPException
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

		var localProfile = export.getProfiles().stream()
				.filter(profile -> profile.getTrust() == Trust.ULTIMATE)
				.findFirst().orElseThrow(() -> new IllegalArgumentException("No local profile in the profile list"));

		var localLocationId = export.getLocal().getLocation().getLocationId();
		var localLocation = localProfile.getLocations().stream()
				.filter(location -> location.getLocationId().equals(localLocationId))
				.findFirst().orElseThrow(); // XXX: if not found, create new location? should be allowed

		createOwnProfile(localProfile.getName(), export.getLocal().getProfile().getPgpPrivateKey(), localProfile.getPgpPublicKeyData());
		createOwnLocation(localLocation.getName(), export.getLocal().getLocation().getPrivateKey(), export.getLocal().getLocation().getPublicKey(), export.getLocal().getLocation().getX509Certificate());
		createOwnIdentity(export.getLocal().getIdentity().getName(), export.getLocal().getIdentity().getPrivateKey(), export.getLocal().getIdentity().getPublicKey());

		createProfiles(export.getProfiles());
	}

	@Transactional
	public void importFromRs(MultipartFile file, String locationName, String password) throws IOException, PGPException, InvalidKeyException
	{
		if (file == null)
		{
			throw new IllegalArgumentException("RS keyring is empty");
		}

		if (file.getSize() >= BACKUP_MAX_SIZE)
		{
			throw new IllegalArgumentException("RS keyring is too big");
		}

		if (StringUtils.isEmpty(locationName))
		{
			throw new IllegalArgumentException("Location name is empty");
		}

		if (StringUtils.isEmpty(password))
		{
			password = "";
		}

		var secretRingCollection = new JcaPGPSecretKeyRingCollection(file.getInputStream());
		var secretRing = secretRingCollection.getKeyRings().next();
		var secretKey = secretRing.getSecretKey();

		var digestCalculator = new JcaPGPDigestCalculatorProviderBuilder().build();
		var keyDecryptor = new JcePBESecretKeyDecryptorBuilder(digestCalculator);

		var id = secretKey.getPublicKey().getUserIDs().next();
		var profileName = cleanupProfileName(id);

		PGPKeyPair keyPair;

		// Decrypt
		try
		{
			keyPair = secretKey.extractKeyPair(keyDecryptor.build(password.toCharArray()));
		}
		catch (PGPException e)
		{
			throw new IllegalArgumentException("Wrong password", e);
		}

		// End encrypt again with an empty password because we use a different security model
		var newSecretKey = PGP.encryptKeyPair(keyPair, id);

		createOwnProfile(cleanupProfileName(id),
				newSecretKey.getEncoded(),
				newSecretKey.getPublicKey().getEncoded());

		locationService.generateOwnLocation(locationName);
		identityRsService.generateOwnIdentity(profileName, true);
	}

	private String cleanupProfileName(String profileName)
	{
		return profileName.replace(" (Generated by Retroshare) <>", "");
	}

	private void createOwnProfile(String name, byte[] privateKey, byte[] publicKey) throws InvalidKeyException, IOException
	{
		var pgpSecretKey = PGP.getPGPSecretKey(privateKey);
		var pgpPublicKey = PGP.getPGPPublicKey(publicKey);
		profileService.createOwnProfile(name, pgpSecretKey, pgpPublicKey);
	}

	private void createOwnLocation(String name, byte[] privateKey, byte[] publicKey, byte[] x509Certificate) throws NoSuchAlgorithmException, InvalidKeySpecException, CertificateException
	{
		var keyPair = new KeyPair(RSA.getPublicKey(publicKey), RSA.getPrivateKey(privateKey));
		locationService.createOwnLocation(name, keyPair, x509Certificate);
	}

	private void createOwnIdentity(String name, byte[] privateKey, byte[] publicKey) throws NoSuchAlgorithmException, InvalidKeySpecException, PGPException, IOException
	{
		var keyPair = new KeyPair(RSA.getPublicKey(publicKey), RSA.getPrivateKey(privateKey));
		identityRsService.createOwnIdentity(name, keyPair);
	}

	private void createProfiles(List<io.xeres.app.database.model.profile.Profile> profiles) throws InvalidKeyException
	{
		for (io.xeres.app.database.model.profile.Profile profile : profiles)
		{
			if (profile.getTrust() != Trust.ULTIMATE)
			{
				var pgpPublicKey = PGP.getPGPPublicKey(profile.getPgpPublicKeyData());
				var createdProfile = io.xeres.app.database.model.profile.Profile.createProfile(
						profile.getName(), profile.getPgpIdentifier(), pgpPublicKey.getCreationTime().toInstant(), new ProfileFingerprint(pgpPublicKey.getFingerprint()), pgpPublicKey);
				profile.getLocations().forEach(createdProfile::addLocation);
				createdProfile.setAccepted(true);
				profileService.createOrUpdateProfile(createdProfile);
			}
		}
	}
}
