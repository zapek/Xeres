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

import io.xeres.app.crypto.hash.sha1.Sha1MessageDigest;
import io.xeres.app.crypto.pgp.PGP;
import io.xeres.app.database.DatabaseSession;
import io.xeres.app.database.DatabaseSessionManager;
import io.xeres.app.database.model.gxs.GxsCircleType;
import io.xeres.app.database.model.gxs.GxsGroupItem;
import io.xeres.app.database.model.gxs.GxsMessageItem;
import io.xeres.app.database.model.gxs.GxsPrivacyFlags;
import io.xeres.app.database.model.profile.Profile;
import io.xeres.app.net.peer.PeerConnection;
import io.xeres.app.net.peer.PeerConnectionManager;
import io.xeres.app.service.IdentityService;
import io.xeres.app.service.ProfileService;
import io.xeres.app.service.ResourceCreationState;
import io.xeres.app.service.SettingsService;
import io.xeres.app.service.notification.contact.ContactNotificationService;
import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.service.RsServiceRegistry;
import io.xeres.app.xrs.service.RsServiceType;
import io.xeres.app.xrs.service.gxs.AuthenticationRequirements;
import io.xeres.app.xrs.service.gxs.GxsRsService;
import io.xeres.app.xrs.service.gxs.GxsTransactionManager;
import io.xeres.app.xrs.service.gxs.GxsUpdateService;
import io.xeres.app.xrs.service.identity.item.IdentityGroupItem;
import io.xeres.common.dto.identity.IdentityConstants;
import io.xeres.common.id.*;
import io.xeres.common.identity.Type;
import io.xeres.common.util.ExecutorUtils;
import jakarta.persistence.EntityNotFoundException;
import net.coobird.thumbnailator.Thumbnails;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.SignatureException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

import static io.xeres.app.service.ResourceCreationState.*;
import static io.xeres.app.xrs.service.RsServiceType.GXSID;
import static io.xeres.app.xrs.service.gxs.AuthenticationRequirements.Flags.CHILD_AUTHOR;
import static io.xeres.app.xrs.service.gxs.AuthenticationRequirements.Flags.ROOT_AUTHOR;

@Component
public class IdentityRsService extends GxsRsService<IdentityGroupItem, GxsMessageItem>
{
	private static final long IMAGE_MAX_SIZE = 1024 * 1024 * 10L; // 10 MB
	private static final int IMAGE_WIDTH = 128;
	private static final int IMAGE_HEIGHT = 128;

	private static final Duration PENDING_VALIDATION_START = Duration.ofSeconds(60);
	private static final Duration PENDING_VALIDATION_DELAY = Duration.ofSeconds(2);
	private static final Duration PENDING_VALIDATION_FULL_QUERY_DELAY = Duration.ofSeconds(60);

	private static final int PENDING_IDENTITIES_MAX = 32;

	private ScheduledExecutorService executorService;
	private final DatabaseSessionManager databaseSessionManager;
	private final Queue<IdentityGroupItem> pendingIdentities = new ArrayDeque<>(PENDING_IDENTITIES_MAX);
	private Instant lastFullQuery = Instant.EPOCH;

	private final IdentityService identityService;
	private final SettingsService settingsService;
	private final ProfileService profileService;
	private final GxsUpdateService<IdentityGroupItem, GxsMessageItem> gxsUpdateService;
	private final ContactNotificationService contactNotificationService;

	private enum ValidationResult
	{
		VALID,
		INVALID,
		NOT_FOUND
	}

	public IdentityRsService(RsServiceRegistry rsServiceRegistry, PeerConnectionManager peerConnectionManager, GxsTransactionManager gxsTransactionManager, DatabaseSessionManager databaseSessionManager, IdentityService identityService, SettingsService settingsService, ProfileService profileService, IdentityManager identityManager, GxsUpdateService<IdentityGroupItem, GxsMessageItem> gxsUpdateService, ContactNotificationService contactNotificationService)
	{
		super(rsServiceRegistry, peerConnectionManager, gxsTransactionManager, databaseSessionManager, identityManager, gxsUpdateService);
		this.databaseSessionManager = databaseSessionManager;
		this.identityService = identityService;
		this.settingsService = settingsService;
		this.profileService = profileService;
		this.gxsUpdateService = gxsUpdateService;
		this.contactNotificationService = contactNotificationService;
	}

	@Override
	public RsServiceType getServiceType()
	{
		return GXSID;
	}

	@Override
	protected AuthenticationRequirements getAuthenticationRequirements()
	{
		return new AuthenticationRequirements.Builder()
				.withPublic(EnumSet.of(ROOT_AUTHOR, CHILD_AUTHOR))
				.withRestricted(EnumSet.of(ROOT_AUTHOR, CHILD_AUTHOR))
				.withPrivate(EnumSet.of(ROOT_AUTHOR, CHILD_AUTHOR))
				.build();
	}

	@Override
	public void initialize()
	{
		super.initialize();

		executorService = ExecutorUtils.createFixedRateExecutor(this::checkForProfileValidation,
				getInitPriority().getMaxTime() + PENDING_VALIDATION_START.toSeconds(),
				PENDING_VALIDATION_DELAY.toSeconds());
	}

	@Override
	public void cleanup()
	{
		super.cleanup();
		ExecutorUtils.cleanupExecutor(executorService);
	}

	private void checkForProfileValidation()
	{
		var identity = pendingIdentities.poll();
		if (identity == null)
		{
			// Search for identities not validated yet
			var now = Instant.now();
			if (lastFullQuery.isBefore(now))
			{
				try (var ignored = new DatabaseSession(databaseSessionManager))
				{
					pendingIdentities.addAll(identityService.findIdentitiesToValidate(PENDING_IDENTITIES_MAX));
					lastFullQuery = now.plus(PENDING_VALIDATION_FULL_QUERY_DELAY);
				}
			}
		}
		else
		{
			var identityServiceStorage = new IdentityServiceStorage(identity.getServiceString()); // We allow wrong service strings

			try (var ignored = new DatabaseSession(databaseSessionManager))
			{
				switch (validate(identity, identityServiceStorage))
				{
					case VALID ->
					{
						identityServiceStorage.updateIdScore(true, true);
						identity.setNextValidation(null);
						identity.setServiceString(identityServiceStorage.out());
						linkWithProfileIfFound(identity, identityServiceStorage.getPgpIdentifier());
						identityService.save(identity);
					}
					case INVALID ->
					{
						identityService.delete(identity);
						contactNotificationService.removeIdentities(List.of(identity)); // This might be re-added immediately by discovery if it's on a friend. RS has the same problem
					}
					case NOT_FOUND ->
					{
						identityServiceStorage.updateIdScore(true, false);
						identity.setNextValidation(identityServiceStorage.computeNextValidationAttempt());
						identity.setServiceString(identityServiceStorage.out());
						identityService.save(identity);
					}
				}
			}
		}
	}

	private ValidationResult validate(IdentityGroupItem identity, IdentityServiceStorage identityServiceStorage)
	{
		var pgpId = PGP.getIssuer(identity.getProfileSignature());
		if (pgpId == 0)
		{
			log.error("Found anonymous signature. Brute forcing it is not supported.");
			return ValidationResult.INVALID;
		}
		identityServiceStorage.setPgpIdentifier(pgpId);

		var profile = profileService.findProfileByPgpIdentifier(pgpId).orElse(null);
		if (profile == null)
		{
			log.debug("PGP profile not found for identity {}, retrying later", identity);
			return ValidationResult.NOT_FOUND;
		}

		var computedHash = makeProfileHash(identity.getGxsId(), profile.getProfileFingerprint());
		if (!identity.getProfileHash().equals(computedHash))
		{
			log.error("Wrong profile hash for identity {}", identity);
			return ValidationResult.INVALID;
		}

		try
		{
			PGP.verify(PGP.getPGPPublicKey(profile.getPgpPublicKeyData()), identity.getProfileSignature(), new ByteArrayInputStream(computedHash.getBytes()));
			log.debug("Successful PGP profile validation for identity {}", identity);
		}
		catch (IOException | SignatureException | PGPException | InvalidKeyException e)
		{
			log.error("Profile signature verification failed for identity {}: {}", identity, e.getMessage());
			return ValidationResult.INVALID;
		}
		return ValidationResult.VALID;
	}

	private void linkWithProfileIfFound(IdentityGroupItem identity, long pgpId)
	{
		profileService.findProfileByPgpIdentifier(pgpId).ifPresent(identity::setProfile);
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
		return identityService.findAllSubscribedAndPublishedSince(since);
	}

	@Override
	protected Set<GxsId> onAvailableGroupListResponse(Map<GxsId, Instant> ids)
	{
		// From the received list, we keep all identities that have a more recent publishing date than those
		// we already have. If it's a new identity, we don't want it.
		var existingMap = identityService.findAll(ids.keySet()).stream()
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
		return identityService.findAll(ids);
	}

	@Override
	protected boolean onGroupReceived(IdentityGroupItem identityGroupItem)
	{
		log.debug("Saving id {}", identityGroupItem.getGxsId());
		// XXX: important! there should be some checks to make sure there's no malicious overwrite (probably a simple validation should do as id == fingerprint of key)
		identityGroupItem.setSubscribed(true);
		if (identityGroupItem.getDiffusionFlags().contains(GxsPrivacyFlags.SIGNED_ID))
		{
			identityGroupItem.setNextValidation(Instant.now());
		}
		return true;
	}

	@Override
	protected void onGroupsSaved(List<IdentityGroupItem> items)
	{
		contactNotificationService.addIdentities(items);
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
	protected boolean onMessageReceived(GxsMessageItem item)
	{
		return false; // we don't receive messages
	}

	@Override
	protected void onMessagesSaved(List<GxsMessageItem> items)
	{
		// nothing to do since we don't receive them
	}

	@Transactional
	public ResourceCreationState generateOwnIdentity(String name, boolean signed)
	{
		if (!settingsService.isOwnProfilePresent())
		{
			log.error("Cannot create an identity without a profile; Create a profile first");
			return FAILED;
		}
		if (!settingsService.hasOwnLocation())
		{
			log.error("Cannot create an identity without a location; Create a location first");
			return FAILED;
		}

		if (identityService.hasOwnIdentity())
		{
			return ALREADY_EXISTS;
		}

		var gxsIdGroupItem = createGroup(name);
		try
		{
			createOwnIdentity(gxsIdGroupItem, signed);
		}
		catch (PGPException | IOException e)
		{
			log.error("Couldn't generate identity: {}", e.getMessage());
			return FAILED;
		}
		return CREATED;
	}

	@Transactional
	public long createOwnIdentity(String name, KeyPair keyPair) throws PGPException, IOException
	{
		var gxsIdGroupItem = createGroup(name, keyPair);
		return createOwnIdentity(gxsIdGroupItem, true);
	}

	private long createOwnIdentity(IdentityGroupItem gxsIdGroupItem, boolean signed) throws PGPException, IOException
	{
		gxsIdGroupItem.setType(Type.OWN);

		gxsIdGroupItem.setCircleType(GxsCircleType.PUBLIC);

		log.debug("Own identity's GxsId: {}", gxsIdGroupItem.getGxsId());

		if (signed)
		{
			var ownProfile = profileService.getOwnProfile();
			computeHashAndSignature(gxsIdGroupItem, ownProfile);
			gxsIdGroupItem.setProfile(ownProfile);

			// This is because of some backward compatibility, ideally it should be PUBLIC | REAL_ID
			// PRIVATE is equal to REAL_ID_deprecated
			gxsIdGroupItem.setDiffusionFlags(EnumSet.of(GxsPrivacyFlags.PRIVATE, GxsPrivacyFlags.SIGNED_ID));
			var identityServiceStorage = new IdentityServiceStorage(ownProfile.getPgpIdentifier());
			gxsIdGroupItem.setServiceString(identityServiceStorage.out());
		}
		else
		{
			gxsIdGroupItem.setDiffusionFlags(EnumSet.of(GxsPrivacyFlags.PUBLIC));
			// XXX: what should the serviceString have?
		}

		gxsIdGroupItem.setSubscribed(true);

		return saveIdentity(gxsIdGroupItem, true).getId();
	}

	/**
	 * Fixes a profile signature. Xeres used to generate bugged signatures because of a mistake (upper case GxsId instead of lowercase).
	 * While RS will apparently accept them normally, Xeres will delete them.
	 */
	@Transactional
	public void fixOwnProfile() throws PGPException, IOException
	{
		if (!profileService.hasOwnProfile() || !identityService.hasOwnIdentity())
		{
			return; // Nothing to do. There's no profile/identity yet.
		}
		var ownProfile = profileService.getOwnProfile();
		var ownIdentity = identityService.getOwnIdentity();
		ownIdentity.setProfile(ownProfile);
		computeHashAndSignature(ownIdentity, ownProfile);
		saveIdentity(ownIdentity, true);
	}

	private void computeHashAndSignature(IdentityGroupItem gxsIdGroupItem, Profile profile) throws PGPException, IOException
	{
		var hash = makeProfileHash(gxsIdGroupItem.getGxsId(), profile.getProfileFingerprint());
		gxsIdGroupItem.setProfileHash(hash);
		gxsIdGroupItem.setProfileSignature(makeProfileSignature(PGP.getPGPSecretKey(settingsService.getSecretProfileKey()), hash));
	}

	@Transactional
	public IdentityGroupItem saveIdentity(IdentityGroupItem identityGroupItem)
	{
		return saveIdentity(identityGroupItem, false);
	}

	private IdentityGroupItem saveIdentity(IdentityGroupItem identityGroupItem, boolean updateGroup)
	{
		signGroupIfNeeded(identityGroupItem);
		var savedIdentity = identityService.save(identityGroupItem);
		if (updateGroup)
		{
			gxsUpdateService.setLastServiceGroupsUpdateNow(RsServiceType.GXSID);
		}
		return savedIdentity;
	}

	@Transactional
	public void saveOwnIdentityImage(long id, MultipartFile file) throws IOException
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

		var identity = identityService.findById(id).orElseThrow();

		var out = new ByteArrayOutputStream();
		Thumbnails.of(file.getInputStream())
				.size(IMAGE_WIDTH, IMAGE_HEIGHT)
				.outputFormat("JPEG")
				.toOutputStream(out);

		identity.setImage(out.toByteArray());
		identity.updatePublished();

		saveIdentity(identity, true);
	}

	@Transactional
	public void deleteOwnIdentityImage(long id)
	{
		if (id != IdentityConstants.OWN_IDENTITY_ID)
		{
			throw new EntityNotFoundException("Identity " + id + " is not our own");
		}

		var identity = identityService.findById(id).orElseThrow();
		identity.setImage(null);
		identity.updatePublished();

		saveIdentity(identity, true);
	}

	@Override
	public void shutdown()
	{
		contactNotificationService.shutdown();
	}

	static Sha1Sum makeProfileHash(GxsId gxsId, ProfileFingerprint fingerprint)
	{
		var gxsIdAsciiUpper = Id.toAsciiBytes(gxsId);

		var md = new Sha1MessageDigest();
		md.update(gxsIdAsciiUpper);
		md.update(fingerprint.getBytes());
		return md.getSum();
	}

	private static byte[] makeProfileSignature(PGPSecretKey pgpSecretKey, Sha1Sum hashToSign) throws PGPException, IOException
	{
		var out = new ByteArrayOutputStream();
		PGP.sign(pgpSecretKey, new ByteArrayInputStream(hashToSign.getBytes()), out, PGP.Armor.NONE);
		return out.toByteArray();
	}
}
