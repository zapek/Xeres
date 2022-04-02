/*
 * Copyright (c) 2019-2021 by David Gerber - https://zapek.com
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
import io.xeres.app.database.repository.GxsIdRepository;
import io.xeres.app.xrs.service.RsServiceType;
import io.xeres.app.xrs.service.gxsid.item.GxsIdGroupItem;
import io.xeres.common.dto.identity.IdentityConstants;
import io.xeres.common.gxsid.Type;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.Id;
import io.xeres.common.id.ProfileFingerprint;
import io.xeres.common.id.Sha1Sum;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class IdentityService
{
	private static final Logger log = LoggerFactory.getLogger(IdentityService.class);

	private final GxsIdRepository gxsIdRepository;
	private final PrefsService prefsService;
	private final ProfileService profileService;
	private final GxsExchangeService gxsExchangeService;

	public IdentityService(GxsIdRepository gxsIdRepository, PrefsService prefsService, ProfileService profileService, GxsExchangeService gxsExchangeService)
	{
		this.gxsIdRepository = gxsIdRepository;
		this.prefsService = prefsService;
		this.profileService = profileService;
		this.gxsExchangeService = gxsExchangeService;
	}

	@Transactional
	public long createOwnIdentity(String name, boolean signed) throws CertificateException, PGPException, IOException
	{
		if (!prefsService.isOwnProfilePresent())
		{
			throw new CertificateException("Cannot create an identity without a profile; Create a profile first");
		}
		if (!prefsService.hasOwnLocation())
		{
			throw new IllegalArgumentException("Cannot create an identity without a location; Create a location first");
		}

		var adminKeyPair = RSA.generateKeys(2048);
		var publishingKeyPair = RSA.generateKeys(2048);

		var adminPrivateKey = (RSAPrivateKey) adminKeyPair.getPrivate();
		var adminPublicKey = (RSAPublicKey) adminKeyPair.getPublic();

		var publishingPrivateKey = (RSAPrivateKey) publishingKeyPair.getPrivate();
		var publishingPublicKey = (RSAPublicKey) publishingKeyPair.getPublic();

		// The GxsId is from the publishing (identity) key
		var gxsId = makeGxsId(
				getAsOneComplement(publishingPublicKey.getModulus()),
				getAsOneComplement(publishingPublicKey.getPublicExponent()));

		var gxsIdGroupItem = new GxsIdGroupItem(gxsId, name);
		gxsIdGroupItem.setType(Type.OWN);
		gxsIdGroupItem.setAdminPrivateKey(adminPrivateKey);
		gxsIdGroupItem.setAdminPublicKey(adminPublicKey);

		// XXX: might not be needed for anonymous identities
		gxsIdGroupItem.setPublishingPrivateKey(publishingPrivateKey);
		gxsIdGroupItem.setPublishingPublicKey(publishingPublicKey);

		gxsIdGroupItem.setCircleType(GxsCircleType.PUBLIC);

		if (signed)
		{
			var ownProfile = profileService.getOwnProfile();
			var hash = makeProfileHash(gxsId, ownProfile.getProfileFingerprint());
			gxsIdGroupItem.setProfileHash(hash);
			gxsIdGroupItem.setProfileSignature(makeProfileSignature(PGP.getPGPSecretKey(prefsService.getSecretProfileKey()), hash));

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

		gxsIdGroupItem = gxsIdRepository.save(gxsIdGroupItem);

		gxsExchangeService.setLastServiceUpdate(RsServiceType.GXSID, gxsIdGroupItem.getPublished());

		return gxsIdGroupItem.getId();
	}

	public GxsIdGroupItem getOwnIdentity() // XXX: temporary, we'll have several identities later
	{
		return gxsIdRepository.findById(IdentityConstants.OWN_IDENTITY_ID).orElseThrow(() -> new IllegalStateException("Missing own gxsId"));
	}

	public Optional<GxsIdGroupItem> findById(long id)
	{
		return gxsIdRepository.findById(id);
	}

	public void saveIdentity(GxsIdGroupItem gxsIdGroupItem)
	{
		// XXX: important! there should be some checks to make sure there's no malicious overwrite (probably a simple validation should do as id == fingerprint of key)
		var gxsIdGroup = gxsIdRepository.findByGxsId(gxsIdGroupItem.getGxsId()).orElse(gxsIdGroupItem);
		gxsIdRepository.save(gxsIdGroup);
	}

	public Collection<GxsIdGroupItem> findAllByName(String name)
	{
		return gxsIdRepository.findAllByName(name);
	}

	public Optional<GxsIdGroupItem> findByGxsId(GxsId gxsId)
	{
		return gxsIdRepository.findByGxsId(gxsId);
	}

	public List<GxsIdGroupItem> getAll()
	{
		return gxsIdRepository.findAll();
	}

	public List<GxsIdGroupItem> findAll(Set<GxsId> gxsIds)
	{
		return gxsIdRepository.findAllByGxsIdIn(gxsIds);
	}

	public byte[] signData(GxsIdGroupItem gxsIdGroupItem, byte[] data)
	{
		return RSA.sign(data, gxsIdGroupItem.getPublishingPrivateKey());
	}

	private byte[] getAsOneComplement(BigInteger number)
	{
		var array = number.toByteArray();
		if (array[0] == 0)
		{
			array = Arrays.copyOfRange(array, 1, array.length);
		}
		return array;
	}

	private GxsId makeGxsId(byte[] modulus, byte[] exponent)
	{
		var sha1sum = new byte[Sha1Sum.LENGTH];

		Digest digest = new SHA1Digest();
		digest.update(modulus, 0, modulus.length);
		digest.update(exponent, 0, exponent.length);
		digest.doFinal(sha1sum, 0);

		// Copy the first 16 bytes of the sha1 sum to get the GxsId
		return new GxsId(Arrays.copyOfRange(sha1sum, 0, GxsId.LENGTH));
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
