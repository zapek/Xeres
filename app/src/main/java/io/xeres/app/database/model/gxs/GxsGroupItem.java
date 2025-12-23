/*
 * Copyright (c) 2019-2025 by David Gerber - https://zapek.com
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

package io.xeres.app.database.model.gxs;

import io.netty.buffer.ByteBuf;
import io.xeres.app.crypto.rsa.RSA;
import io.xeres.app.xrs.common.SecurityKey;
import io.xeres.app.xrs.common.Signature;
import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.serialization.FieldSize;
import io.xeres.app.xrs.serialization.SerializationFlags;
import io.xeres.app.xrs.serialization.TlvType;
import io.xeres.app.xrs.service.gxs.item.DynamicServiceType;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.LocationIdentifier;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.time.Instant;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static io.xeres.app.database.model.gxs.GxsConstants.GXS_ITEM_MAX_SIZE;
import static io.xeres.app.xrs.common.SecurityKey.Flags.*;
import static io.xeres.app.xrs.serialization.Serializer.*;

@Entity(name = "gxs_group")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class GxsGroupItem extends Item implements GxsMetaAndData, DynamicServiceType
{
	private static final Logger log = LoggerFactory.getLogger(GxsGroupItem.class);

	private static final int API_VERSION_1 = 0x0000;
	private static final int API_VERSION_2 = 0xaf01;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Embedded
	@AttributeOverride(name = "identifier", column = @Column(name = "gxs_id"))
	private GxsId gxsId;

	@Embedded
	@AttributeOverride(name = "identifier", column = @Column(name = "original_gxs_id"))
	private GxsId originalGxsId;

	@NotNull
	private String name;

	private Set<GxsPrivacyFlags> diffusionFlags = EnumSet.noneOf(GxsPrivacyFlags.class);

	private Set<GxsSignatureFlags> signatureFlags = EnumSet.noneOf(GxsSignatureFlags.class); // what signatures are required for parent and child messages

	private Instant published;

	@Embedded
	@AttributeOverride(name = "identifier", column = @Column(name = "author"))
	private GxsId author; // author of the group, all 0 if anonymous

	@Embedded
	@AttributeOverride(name = "identifier", column = @Column(name = "circle_id"))
	private GxsId circleId; // id of the circle to which the group is restricted

	private GxsCircleType circleType = GxsCircleType.UNKNOWN;

	private int authenticationFlags; // not used yet?

	@Embedded
	@AttributeOverride(name = "identifier", column = @Column(name = "parent_id"))
	private GxsId parentId;

	// below is local data (stored in the database only)
	private boolean subscribed;

	private int popularity; // number of friends subscribers
	private int visibleMessageCount; // maximum messages reported by friends
	private Instant lastPosted; // timestamp for last message

	private int status; // GXS_GRP_STATUS_*

	// service specific storage (not synced, but they are serialized though)
	private String serviceString;

	@Embedded
	@AttributeOverride(name = "identifier", column = @Column(name = "originator"))
	private LocationIdentifier originator;

	@Embedded
	@AttributeOverride(name = "identifier", column = @Column(name = "internal_circle"))
	private GxsId internalCircle;

	// the publishing key is used for both DISTRIBUTION_PUBLISHING and DISTRIBUTION_IDENTITY

	@ElementCollection
	private final Set<SecurityKey> privateKeys = HashSet.newHashSet(2);

	@ElementCollection
	private final Set<SecurityKey> publicKeys = HashSet.newHashSet(2);

	@ElementCollection
	private final Set<Signature> signatures = HashSet.newHashSet(2);

	@Transient
	private int serviceType;

	@Override
	public int getServiceType()
	{
		return serviceType;
	}

	@Override
	public void setServiceType(int serviceType)
	{
		this.serviceType = serviceType;
	}

	public long getId()
	{
		return id;
	}

	public void setId(long id)
	{
		this.id = id;
	}

	public GxsId getGxsId()
	{
		return gxsId;
	}

	public void setGxsId(GxsId gxsId)
	{
		this.gxsId = gxsId;
	}

	public GxsId getOriginalGxsId()
	{
		return originalGxsId;
	}

	public void setOriginalGxsId(GxsId originalGxsId)
	{
		this.originalGxsId = originalGxsId;
	}

	public @NotNull String getName()
	{
		return name;
	}

	public void setName(@NotNull String name)
	{
		this.name = name;
	}

	public Set<GxsPrivacyFlags> getDiffusionFlags()
	{
		return diffusionFlags;
	}

	public void setDiffusionFlags(Set<GxsPrivacyFlags> diffusionFlags)
	{
		this.diffusionFlags = diffusionFlags;
	}

	public Set<GxsSignatureFlags> getSignatureFlags()
	{
		return signatureFlags;
	}

	public void setSignatureFlags(Set<GxsSignatureFlags> signatureFlags)
	{
		this.signatureFlags = signatureFlags;
	}

	public Instant getPublished()
	{
		return published;
	}

	public void updatePublished()
	{
		published = Instant.now();
	}

	public GxsId getAuthor()
	{
		return author;
	}

	public void setAuthor(GxsId author)
	{
		this.author = author;
	}

	public GxsId getCircleId()
	{
		return circleId;
	}

	public void setCircleId(GxsId circleId)
	{
		this.circleId = circleId;
	}

	public GxsCircleType getCircleType()
	{
		return circleType;
	}

	public void setCircleType(GxsCircleType circleType)
	{
		this.circleType = circleType;
	}

	public int getAuthenticationFlags()
	{
		return authenticationFlags;
	}

	public void setAuthenticationFlags(int authenticationFlags)
	{
		this.authenticationFlags = authenticationFlags;
	}

	public GxsId getParentId()
	{
		return parentId;
	}

	public void setParentId(GxsId parentId)
	{
		this.parentId = parentId;
	}

	public boolean isSubscribed()
	{
		return subscribed;
	}

	public void setSubscribed(boolean subscribed)
	{
		this.subscribed = subscribed;
	}

	public int getPopularity()
	{
		return popularity;
	}

	public void setPopularity(int popularity)
	{
		this.popularity = popularity;
	}

	public int getVisibleMessageCount()
	{
		return visibleMessageCount;
	}

	public void setVisibleMessageCount(int visibleMessageCount)
	{
		this.visibleMessageCount = visibleMessageCount;
	}

	public Instant getLastPosted()
	{
		return lastPosted;
	}

	public void setLastPosted(Instant lastPosted)
	{
		this.lastPosted = lastPosted;
	}

	public int getStatus()
	{
		return status;
	}

	public void setStatus(int status)
	{
		this.status = status;
	}

	public String getServiceString()
	{
		return serviceString;
	}

	public void setServiceString(String serviceString)
	{
		this.serviceString = serviceString;
	}

	public LocationIdentifier getOriginator()
	{
		return originator;
	}

	public void setOriginator(LocationIdentifier originator)
	{
		this.originator = originator;
	}

	public GxsId getInternalCircle()
	{
		return internalCircle;
	}

	public void setInternalCircle(GxsId internalCircle)
	{
		this.internalCircle = internalCircle;
	}

	/**
	 * Checks if it comes from an external source (meaning: not our own).
	 *
	 * @return true if coming from someone else
	 */
	public boolean isExternal()
	{
		return privateKeys.stream()
				.noneMatch(securityKey -> securityKey.getFlags().containsAll(Set.of(DISTRIBUTION_ADMIN, TYPE_FULL)));
	}

	public PrivateKey getAdminPrivateKey()
	{
		var privateKey = privateKeys.stream()
				.filter(securityKey -> securityKey.getFlags().containsAll(Set.of(DISTRIBUTION_ADMIN, TYPE_FULL)))
				.findFirst().orElseThrow();

		try
		{
			return RSA.getPrivateKeyFromPkcs1(privateKey.getData());
		}
		catch (NoSuchAlgorithmException | InvalidKeySpecException | IOException e)
		{
			throw new IllegalArgumentException("Cannot read PrivateKey from database: " + e.getMessage(), e);
		}
	}

	public void setAdminKeys(PrivateKey privateKey, PublicKey publicKey, Instant validFrom, Instant validTo)
	{
		var keyId = getGxsId();
		if (keyId == null)
		{
			throw new IllegalStateException("GxsGroupItem has no GxsId for the private key");
		}

		try
		{
			var privateData = RSA.getPrivateKeyAsPkcs1(privateKey);
			var publicData = RSA.getPublicKeyAsPkcs1(publicKey);

			privateKeys.add(new SecurityKey(keyId, EnumSet.of(DISTRIBUTION_ADMIN, TYPE_FULL), validFrom, validTo, privateData));
			publicKeys.add(new SecurityKey(keyId, EnumSet.of(DISTRIBUTION_ADMIN, TYPE_PUBLIC_ONLY), validFrom, validTo, publicData));
		}
		catch (IOException e)
		{
			throw new IllegalArgumentException("Cannot read PrivateKey from database: " + e.getMessage(), e);
		}
	}

	public boolean hasAdminPublicKey()
	{
		return publicKeys.stream()
				.anyMatch(securityKey -> isAdminKey(securityKey) && isValidKey(securityKey));
	}

	public PublicKey getAdminPublicKey()
	{
		var publicKey = publicKeys.stream()
				.filter(securityKey -> isAdminKey(securityKey) && isValidKey(securityKey))
				.findFirst().orElseThrow();

		try
		{
			return RSA.getPublicKeyFromPkcs1(publicKey.getData());
		}
		catch (NoSuchAlgorithmException | InvalidKeySpecException | IOException e)
		{
			throw new IllegalArgumentException("Cannot read PublicKey from database: " + e.getMessage(), e);
		}
	}

	private static boolean isAdminKey(SecurityKey securityKey)
	{
		return securityKey.getFlags().containsAll(Set.of(DISTRIBUTION_ADMIN, TYPE_PUBLIC_ONLY));
	}

	private boolean isValidKey(SecurityKey securityKey)
	{
		if (securityKey.getValidFrom().isAfter(getPublished()))
		{
			log.warn("Key {} has an invalid creation date that is less recent than the group's creation", securityKey);
			return false;
		}
		return true;
	}

	// TODO: add publishing key accessors as well

	public byte[] getAdminSignature()
	{
		return signatures.stream()
				.filter(signature -> signature.getType() == Signature.Type.ADMIN)
				.findFirst().orElseThrow().getData();
	}

	public void setAdminSignature(byte[] adminSignature)
	{
		Objects.requireNonNull(gxsId);
		signatures.stream()
				.filter(signature -> signature.getType() == Signature.Type.ADMIN)
				.findFirst().ifPresent(signatures::remove); // XXX: hack! This is caused because it shouldn't be a set to begin with!
		var signature = new Signature(Signature.Type.ADMIN, gxsId, adminSignature);
		signatures.add(signature);
	}

	public byte[] getAuthorSignature()
	{
		return signatures.stream()
				.filter(signature -> signature.getType() == Signature.Type.AUTHOR)
				.findFirst()
				.map(Signature::getData).orElse(null);
	}

	public void setAuthorSignature(byte[] authorSignature)
	{
		Objects.requireNonNull(authorSignature);
		signatures.stream()
				.filter(signature -> signature.getType() == Signature.Type.AUTHOR)
				.findFirst().ifPresent(signatures::remove); // XXX: hack! This is caused because it shouldn't be a set to begin with!
		var signature = new Signature(Signature.Type.AUTHOR, author, authorSignature);
		signatures.add(signature);
	}

	@Override
	public int writeMetaObject(ByteBuf buf, Set<SerializationFlags> serializationFlags)
	{
		var size = 0;

		size += serialize(buf, API_VERSION_2); // current RS API
		var sizeOffset = buf.writerIndex();
		size += serialize(buf, 0); // write size at the end
		size += serialize(buf, gxsId, GxsId.class);
		size += serialize(buf, originalGxsId, GxsId.class);
		size += serialize(buf, parentId, GxsId.class);
		size += serialize(buf, TlvType.STR_NONE, name);
		size += serialize(buf, diffusionFlags, FieldSize.INTEGER);
		size += serialize(buf, (int) published.getEpochSecond());
		size += serialize(buf, circleType);
		size += serialize(buf, authenticationFlags);
		size += serialize(buf, author, GxsId.class);
		size += serialize(buf, TlvType.STR_NONE, serviceString);
		size += serialize(buf, circleId, GxsId.class);
		size += serialize(buf, TlvType.SIGNATURE_SET, serializationFlags.contains(SerializationFlags.SIGNATURE) ? new HashSet<>() : signatures);
		size += serialize(buf, TlvType.SECURITY_KEY_SET, publicKeys);
		size += serialize(buf, signatureFlags, FieldSize.INTEGER);
		buf.setInt(sizeOffset, size); // write total size

		return size;
	}

	@Override
	public void readMetaObject(ByteBuf buf)
	{
		var apiVersion = deserializeInt(buf);
		if (apiVersion != API_VERSION_1 && apiVersion != API_VERSION_2)
		{
			throw new IllegalArgumentException("Unsupported API version " + apiVersion);
		}
		var size = deserializeInt(buf); // the size
		if (size > GXS_ITEM_MAX_SIZE)
		{
			throw new IllegalArgumentException("Gxs group meta size " + size + " is bigger than the maximum of " + GXS_ITEM_MAX_SIZE);
		}
		gxsId = (GxsId) deserializeIdentifier(buf, GxsId.class);
		originalGxsId = (GxsId) deserializeIdentifier(buf, GxsId.class);
		parentId = (GxsId) deserializeIdentifier(buf, GxsId.class);
		name = (String) deserialize(buf, TlvType.STR_NONE);
		diffusionFlags = deserializeEnumSet(buf, GxsPrivacyFlags.class, FieldSize.INTEGER);
		published = Instant.ofEpochSecond(deserializeInt(buf));
		circleType = deserializeEnum(buf, GxsCircleType.class);
		authenticationFlags = deserializeInt(buf);
		author = (GxsId) deserializeIdentifier(buf, GxsId.class);
		serviceString = (String) deserialize(buf, TlvType.STR_NONE);
		circleId = (GxsId) deserializeIdentifier(buf, GxsId.class);
		deserializeSignatures(buf);
		deserializeSecurityKeySet(buf);
		if (apiVersion == API_VERSION_2)
		{
			signatureFlags = deserializeEnumSet(buf, GxsSignatureFlags.class, FieldSize.INTEGER);
		}
	}

	private void deserializeSecurityKeySet(ByteBuf buf)
	{
		@SuppressWarnings("unchecked") var securityKeys = (Set<SecurityKey>) deserialize(buf, TlvType.SECURITY_KEY_SET);
		securityKeys.forEach(securityKey -> {
			if (securityKey.getFlags().contains(TYPE_PUBLIC_ONLY))
			{
				publicKeys.add(securityKey);
			}
			else
			{
				log.warn("Peer tried to send a private key, ignoring");
			}
		});
	}

	private void deserializeSignatures(ByteBuf buf)
	{
		@SuppressWarnings("unchecked") var signatureSet = (Set<Signature>) deserialize(buf, TlvType.SIGNATURE_SET);
		signatures.clear();
		signatureSet.forEach(signature -> {
			if (signature.getType() == Signature.Type.ADMIN || signature.getType() == Signature.Type.AUTHOR)
			{
				signatures.add(signature);
			}
			else
			{
				log.warn("Unknown signature type: {}", signature.getType());
			}
		});
	}

	@Override
	public GxsGroupItem clone()
	{
		return (GxsGroupItem) super.clone();
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o)
		{
			return true;
		}
		if (o == null || getClass() != o.getClass())
		{
			return false;
		}
		GxsGroupItem that = (GxsGroupItem) o;
		return Objects.equals(gxsId, that.gxsId);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(gxsId);
	}

	@Override
	public String toString()
	{
		return "GxsGroupItem{" +
				"id=" + id +
				", gxsId=" + gxsId +
				", name='" + name + '\'' +
				", flags=" + diffusionFlags +
				", signatureFlags=" + signatureFlags +
				", published=" + published +
				", author=" + author +
				", circleId=" + circleId +
				", circleType=" + circleType +
				", authenticationFlags=" + authenticationFlags +
				", parentId=" + parentId +
				", isSubscribed=" + subscribed +
				", popularity=" + popularity +
				", visibleMessageCount=" + visibleMessageCount +
				", lastPosted=" + lastPosted +
				", status=" + status +
				", serviceString='" + serviceString + '\'' +
				", originator=" + originator +
				", internalCircle=" + internalCircle +
				'}';
	}
}
