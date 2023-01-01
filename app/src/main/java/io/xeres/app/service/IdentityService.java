/*
 * Copyright (c) 2019-2023 by David Gerber - https://zapek.com
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

import io.xeres.app.crypto.pgp.PGP;
import io.xeres.app.crypto.pgp.PGP.Armor;
import io.xeres.app.crypto.rsa.RSA;
import io.xeres.app.database.model.gxs.GxsCircleType;
import io.xeres.app.database.model.gxs.GxsPrivacyFlags;
import io.xeres.app.database.model.profile.Profile;
import io.xeres.app.database.repository.GxsIdentityRepository;
import io.xeres.app.xrs.service.RsServiceType;
import io.xeres.app.xrs.service.identity.item.IdentityGroupItem;
import io.xeres.common.dto.identity.IdentityConstants;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.Id;
import io.xeres.common.id.ProfileFingerprint;
import io.xeres.common.id.Sha1Sum;
import io.xeres.common.identity.Type;
import jakarta.persistence.EntityNotFoundException;
import net.coobird.thumbnailator.Thumbnails;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class IdentityService
{
	private static final Logger log = LoggerFactory.getLogger(IdentityService.class);

	private static final long IMAGE_MAX_SIZE = 1024 * 1024 * 10L; // 10 MB
	private static final int IMAGE_WIDTH = 128;
	private static final int IMAGE_HEIGHT = 128;

	private final GxsIdentityRepository gxsIdentityRepository;
	private final SettingsService settingsService;
	private final ProfileService profileService;
	private final GxsExchangeService gxsExchangeService;

	public IdentityService(GxsIdentityRepository gxsIdentityRepository, SettingsService settingsService, ProfileService profileService, GxsExchangeService gxsExchangeService)
	{
		this.gxsIdentityRepository = gxsIdentityRepository;
		this.settingsService = settingsService;
		this.profileService = profileService;
		this.gxsExchangeService = gxsExchangeService;
	}

	@Transactional
	public long createOwnIdentity(String name, boolean signed) throws CertificateException, PGPException, IOException
	{
		if (!settingsService.isOwnProfilePresent())
		{
			throw new CertificateException("Cannot create an identity without a profile; Create a profile first");
		}
		if (!settingsService.hasOwnLocation())
		{
			throw new IllegalArgumentException("Cannot create an identity without a location; Create a location first");
		}

		var adminKeyPair = RSA.generateKeys(2048);

		var adminPrivateKey = (RSAPrivateKey) adminKeyPair.getPrivate();
		var adminPublicKey = (RSAPublicKey) adminKeyPair.getPublic();

		// The GxsId is from the public admin key (n and e)
		var gxsId = RSA.getGxsId(adminPublicKey);

		var gxsIdGroupItem = new IdentityGroupItem(gxsId, name);
		gxsIdGroupItem.setType(Type.OWN);
		gxsIdGroupItem.setAdminPrivateKey(adminPrivateKey);
		gxsIdGroupItem.setAdminPublicKey(adminPublicKey);

		gxsIdGroupItem.setCircleType(GxsCircleType.PUBLIC);

		log.debug("Own identity's GxsId: {}", gxsId);

		if (signed)
		{
			var ownProfile = profileService.getOwnProfile();
			var hash = makeProfileHash(gxsId, ownProfile.getProfileFingerprint());
			gxsIdGroupItem.setProfileHash(hash);
			gxsIdGroupItem.setProfileSignature(makeProfileSignature(PGP.getPGPSecretKey(settingsService.getSecretProfileKey()), hash));

			// This is because of some backward compatibility, ideally it should be PUBLIC | REAL_ID
			// PRIVATE is equal to REAL_ID_deprecated
			gxsIdGroupItem.setDiffusionFlags(EnumSet.of(GxsPrivacyFlags.PRIVATE, GxsPrivacyFlags.SIGNED_ID));
			gxsIdGroupItem.setServiceString(String.format("v2 {P:K:1 I:%s}{T:F:0 P:0 T:0}{R:5 5 0 0}", Id.toString(ownProfile.getPgpIdentifier())));
		}
		else
		{
			gxsIdGroupItem.setDiffusionFlags(EnumSet.of(GxsPrivacyFlags.PUBLIC));
			// XXX: what should the serviceString have?
		}
		return saveIdentity(gxsIdGroupItem).getId();
	}

	public IdentityGroupItem getOwnIdentity() // XXX: temporary, we'll have several identities later
	{
		return gxsIdentityRepository.findById(IdentityConstants.OWN_IDENTITY_ID).orElseThrow(() -> new IllegalStateException("Missing own gxsId"));
	}

	public Optional<IdentityGroupItem> findById(long id)
	{
		return gxsIdentityRepository.findById(id);
	}

	@Transactional
	public void transferIdentity(IdentityGroupItem identityGroupItem)
	{
		identityGroupItem.setId(gxsIdentityRepository.findByGxsId(identityGroupItem.getGxsId()).orElse(identityGroupItem).getId());
		if (Profile.isOwn(identityGroupItem.getId()))
		{
			return; // Don't overwrite our own identity
		}
		// XXX: important! there should be some checks to make sure there's no malicious overwrite (probably a simple validation should do as id == fingerprint of key)
		gxsIdentityRepository.save(identityGroupItem);
	}

	@Transactional
	public IdentityGroupItem saveIdentity(IdentityGroupItem identityGroupItem)
	{
		var savedIdentity = gxsIdentityRepository.save(identityGroupItem);
		gxsExchangeService.setLastServiceUpdate(RsServiceType.GXSID, Instant.now()); // savedEntity.getPublished() is updated *after* the transaction so not usable here
		return savedIdentity;
	}

	public List<IdentityGroupItem> findAllByName(String name)
	{
		return gxsIdentityRepository.findAllByName(name);
	}

	public Optional<IdentityGroupItem> findByGxsId(GxsId gxsId)
	{
		return gxsIdentityRepository.findByGxsId(gxsId);
	}

	public List<IdentityGroupItem> findAllByType(Type type)
	{
		return gxsIdentityRepository.findAllByType(type);
	}

	public List<IdentityGroupItem> getAll()
	{
		return gxsIdentityRepository.findAll();
	}

	public List<IdentityGroupItem> findAll(Set<GxsId> gxsIds)
	{
		return gxsIdentityRepository.findAllByGxsIdIn(gxsIds);
	}

	public List<IdentityGroupItem> findAllPublishedSince(Instant since)
	{
		return gxsIdentityRepository.findAllByPublishedAfter(since);
	}

	@Transactional(propagation = Propagation.NEVER)
	public byte[] signData(IdentityGroupItem identityGroupItem, byte[] data)
	{
		return RSA.sign(data, identityGroupItem.getAdminPrivateKey());
	}

	@Transactional
	public void saveIdentityImage(long id, MultipartFile file) throws IOException
	{
		if (id != IdentityConstants.OWN_IDENTITY_ID)
		{
			throw new EntityNotFoundException("Identity " + id + " is not our own");
		}

		if (file == null)
		{
			throw new IllegalArgumentException("Avatar image is empty");
		}

		if (file.getSize() >= IMAGE_MAX_SIZE)
		{
			throw new IllegalArgumentException("Avatar image size is bigger than " + IMAGE_MAX_SIZE + " bytes");
		}

		var identity = findById(id).orElseThrow();

		var out = new ByteArrayOutputStream();
		Thumbnails.of(file.getInputStream())
				.size(IMAGE_WIDTH, IMAGE_HEIGHT)
				.outputFormat("JPEG")
				.toOutputStream(out);

		identity.setImage(out.toByteArray());
		saveIdentity(identity);
	}

	@Transactional
	public void deleteIdentityImage(long id)
	{
		if (id != IdentityConstants.OWN_IDENTITY_ID)
		{
			throw new EntityNotFoundException("Identity " + id + " is not our own");
		}

		var identity = findById(id).orElseThrow();
		identity.setImage(null);
		saveIdentity(identity);
	}

	private Sha1Sum makeProfileHash(GxsId gxsId, ProfileFingerprint fingerprint)
	{
		var sha1sum = new byte[Sha1Sum.LENGTH];
		var gxsIdAsciiUpper = Id.toAsciiBytesUpperCase(gxsId);

		Digest digest = new SHA1Digest();
		digest.update(gxsIdAsciiUpper, 0, gxsIdAsciiUpper.length);
		digest.update(fingerprint.getBytes(), 0, fingerprint.getLength());
		digest.doFinal(sha1sum, 0);
		return new Sha1Sum(sha1sum);
	}

	private byte[] makeProfileSignature(PGPSecretKey pgpSecretKey, Sha1Sum hashToSign) throws PGPException, IOException
	{
		var out = new ByteArrayOutputStream();
		PGP.sign(pgpSecretKey, new ByteArrayInputStream(hashToSign.getBytes()), out, Armor.NONE);
		return out.toByteArray();
	}
}
