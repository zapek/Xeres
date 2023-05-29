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

package io.xeres.app.xrs.service.identity;

import io.xeres.app.crypto.pgp.PGP;
import io.xeres.app.crypto.rsa.RSA;
import io.xeres.app.database.model.gxs.GxsCircleType;
import io.xeres.app.database.model.gxs.GxsGroupItem;
import io.xeres.app.database.model.gxs.GxsMessageItem;
import io.xeres.app.database.model.gxs.GxsPrivacyFlags;
import io.xeres.app.database.repository.GxsClientUpdateRepository;
import io.xeres.app.database.repository.GxsGroupItemRepository;
import io.xeres.app.database.repository.GxsIdentityRepository;
import io.xeres.app.database.repository.GxsServiceSettingRepository;
import io.xeres.app.net.peer.PeerConnection;
import io.xeres.app.net.peer.PeerConnectionManager;
import io.xeres.app.service.ProfileService;
import io.xeres.app.service.SettingsService;
import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.service.RsServiceRegistry;
import io.xeres.app.xrs.service.RsServiceType;
import io.xeres.app.xrs.service.gxs.GxsRsService;
import io.xeres.app.xrs.service.gxs.GxsTransactionManager;
import io.xeres.app.xrs.service.identity.item.IdentityGroupItem;
import io.xeres.common.dto.identity.IdentityConstants;
import io.xeres.common.id.*;
import io.xeres.common.identity.Type;
import jakarta.persistence.EntityNotFoundException;
import net.coobird.thumbnailator.Thumbnails;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static io.xeres.app.xrs.service.RsServiceType.GXSID;

@Component
public class IdentityRsService extends GxsRsService<IdentityGroupItem, GxsMessageItem>
{
	private static final long IMAGE_MAX_SIZE = 1024 * 1024 * 10L; // 10 MB
	private static final int IMAGE_WIDTH = 128;
	private static final int IMAGE_HEIGHT = 128;

	private final GxsIdentityRepository gxsIdentityRepository;
	private final SettingsService settingsService;
	private final ProfileService profileService;
	private final TransactionTemplate transactionTemplate;

	public IdentityRsService(RsServiceRegistry rsServiceRegistry, PeerConnectionManager peerConnectionManager, GxsTransactionManager gxsTransactionManager, GxsClientUpdateRepository gxsClientUpdateRepository, GxsServiceSettingRepository gxsServiceSettingRepository, GxsIdentityRepository gxsIdentityRepository, SettingsService settingsService, ProfileService profileService, IdentityManager identityManager, GxsGroupItemRepository gxsGroupItemRepository, TransactionTemplate transactionTemplate)
	{
		super(rsServiceRegistry, peerConnectionManager, gxsTransactionManager, gxsClientUpdateRepository, gxsServiceSettingRepository, identityManager, gxsGroupItemRepository, transactionTemplate);
		this.gxsIdentityRepository = gxsIdentityRepository;
		this.settingsService = settingsService;
		this.profileService = profileService;
		this.transactionTemplate = transactionTemplate;
	}

	@Override
	public RsServiceType getServiceType()
	{
		return GXSID;
	}

	@Transactional
	@Override
	public void handleItem(PeerConnection sender, Item item)
	{
		super.handleItem(sender, item); // This is required for the @Transactional to work
	}

	@Override
	protected List<IdentityGroupItem> onAvailableGroupListRequest(PeerConnection recipient, Instant since)
	{
		return findAllSubscribedAndPublishedSince(since);
	}

	@Override
	protected Set<GxsId> onAvailableGroupListResponse(Map<GxsId, Instant> ids)
	{
		// From the received list, we keep all identities that have a more recent publishing date than those
		// we already have. If it's a new identity, we don't want it.
		var existingMap = findAll(ids.keySet()).stream()
				.collect(Collectors.toMap(GxsGroupItem::getGxsId, identityGroupItem -> identityGroupItem.getPublished().truncatedTo(ChronoUnit.SECONDS)));

		ids.entrySet().removeIf(gxsIdInstantEntry -> {
			var existing = existingMap.get(gxsIdInstantEntry.getKey());
			return existing == null || !gxsIdInstantEntry.getValue().isAfter(existing);
		});
		return ids.keySet();
	}

	@Override
	protected List<IdentityGroupItem> onGroupListRequest(Set<GxsId> ids)
	{
		return findAll(ids);
	}

	@Override
	protected boolean onGroupReceived(IdentityGroupItem identityGroupItem)
	{
		log.debug("Saving id {}", identityGroupItem.getGxsId());
		// XXX: important! there should be some checks to make sure there's no malicious overwrite (probably a simple validation should do as id == fingerprint of key)
		identityGroupItem.setSubscribed(true);
		return true;
	}

	@Override
	protected List<GxsMessageItem> onPendingMessageListRequest(PeerConnection recipient, GxsId groupId, Instant since)
	{
		return Collections.emptyList();
	}

	@Override
	protected List<GxsMessageItem> onMessageListRequest(GxsId groupId, Set<MessageId> messageIds)
	{
		return Collections.emptyList();
	}

	@Override
	protected List<MessageId> onMessageListResponse(GxsId groupId, Set<MessageId> messageIds)
	{
		return Collections.emptyList();
	}

	@Override
	protected void onMessageReceived(PeerConnection sender, GxsMessageItem item)
	{
		// we don't receive messages
	}

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

		var gxsIdGroupItem = createGroup(name);
		gxsIdGroupItem.setType(Type.OWN);

		gxsIdGroupItem.setCircleType(GxsCircleType.PUBLIC);

		log.debug("Own identity's GxsId: {}", gxsIdGroupItem.getGxsId());

		if (signed)
		{
			var ownProfile = profileService.getOwnProfile();
			var hash = makeProfileHash(gxsIdGroupItem.getGxsId(), ownProfile.getProfileFingerprint());
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

		gxsIdGroupItem.setSubscribed(true);

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

	public IdentityGroupItem saveIdentity(IdentityGroupItem identityGroupItem)
	{
		signGroupIfNeeded(identityGroupItem);
		var savedIdentity = gxsIdentityRepository.save(identityGroupItem);
		setLastServiceGroupsUpdateNow(RsServiceType.GXSID);
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

	public List<IdentityGroupItem> findAllSubscribedAndPublishedSince(Instant since)
	{
		return gxsIdentityRepository.findAllBySubscribedIsTrueAndPublishedAfter(since);
	}

	@Transactional(propagation = Propagation.NEVER)
	public byte[] signData(IdentityGroupItem identityGroupItem, byte[] data)
	{
		return RSA.sign(data, identityGroupItem.getAdminPrivateKey());
	}

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
		identity.updatePublished();

		saveIdentity(identity);
	}

	public void deleteIdentityImage(long id)
	{
		if (id != IdentityConstants.OWN_IDENTITY_ID)
		{
			throw new EntityNotFoundException("Identity " + id + " is not our own");
		}

		var identity = findById(id).orElseThrow();
		identity.setImage(null);
		identity.updatePublished();

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
		PGP.sign(pgpSecretKey, new ByteArrayInputStream(hashToSign.getBytes()), out, PGP.Armor.NONE);
		return out.toByteArray();
	}
}
