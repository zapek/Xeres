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

package io.xeres.app.database.model.gxs;

import io.netty.buffer.ByteBuf;
import io.xeres.app.crypto.rsa.RSA;
import io.xeres.app.database.converter.GxsPrivacyFlagsConverter;
import io.xeres.app.database.converter.GxsSignatureFlagsConverter;
import io.xeres.app.database.converter.PrivateKeyConverter;
import io.xeres.app.database.converter.PublicKeyConverter;
import io.xeres.app.xrs.common.SecurityKey;
import io.xeres.app.xrs.common.SecurityKeySet;
import io.xeres.app.xrs.common.Signature;
import io.xeres.app.xrs.common.SignatureSet;
import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.serialization.FieldSize;
import io.xeres.app.xrs.serialization.RsSerializable;
import io.xeres.app.xrs.serialization.SerializationFlags;
import io.xeres.app.xrs.serialization.TlvType;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.LocationId;
import org.hibernate.annotations.UpdateTimestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

import static io.xeres.app.xrs.serialization.Serializer.*;

@Entity(name = "gxs_groups")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class GxsGroupItem extends Item implements RsSerializable
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
	@Convert(converter = GxsPrivacyFlagsConverter.class)
	private Set<GxsPrivacyFlags> diffusionFlags;

	@Convert(converter = GxsSignatureFlagsConverter.class)
	private Set<GxsSignatureFlags> signatureFlags; // what signatures are required for parent and child messages

	@UpdateTimestamp
	private Instant published;

	@Embedded
	@AttributeOverride(name = "identifier", column = @Column(name = "author"))
	private GxsId author; // author of the group, all 0 if anonymous

	@Embedded
	@AttributeOverride(name = "identifier", column = @Column(name = "circle_id"))
	private GxsId circleId; // id of the circle to which the group is restricted
	@Enumerated
	private GxsCircleType circleType = GxsCircleType.UNKNOWN;

	private int authenticationFlags; // not used yet?
	@Embedded
	@AttributeOverride(name = "identifier", column = @Column(name = "parent_id"))
	private GxsId parentId;

	// below is local data (stored in the database only)
	private int subscribeFlags;

	private int popularity; // number of friends subscribers
	private int visibleMessageCount; // maximum messages reported by friends
	private Instant lastPosted; // timestamp for last message

	private int status;

	// service specific storage (not synced, but they are serialized though)
	private String serviceString;
	@Embedded
	@AttributeOverride(name = "identifier", column = @Column(name = "originator"))
	private LocationId originator;
	@Embedded
	@AttributeOverride(name = "identifier", column = @Column(name = "internal_circle"))
	private GxsId internalCircle;

	@Convert(converter = PrivateKeyConverter.class)
	private PrivateKey adminPrivateKey;
	@Convert(converter = PublicKeyConverter.class)
	private PublicKey adminPublicKey;

	// the publishing key is used for both DISTRIBUTION_PUBLISHING and DISTRIBUTION_IDENTITY
	@Convert(converter = PrivateKeyConverter.class)
	private PrivateKey publishingPrivateKey;
	@Convert(converter = PublicKeyConverter.class)
	private PublicKey publishingPublicKey;

	@Transient
	private byte[] signature;

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

	public String getName()
	{
		return name;
	}

	public void setName(String name)
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

	public void setPublished(Instant published)
	{
		this.published = published;
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

	public int getSubscribeFlags()
	{
		return subscribeFlags;
	}

	public void setSubscribeFlags(int subscribeFlags)
	{
		this.subscribeFlags = subscribeFlags;
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

	public LocationId getOriginator()
	{
		return originator;
	}

	public void setOriginator(LocationId originator)
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

	public PrivateKey getAdminPrivateKey()
	{
		return adminPrivateKey;
	}

	public void setAdminPrivateKey(PrivateKey adminPrivateKey)
	{
		this.adminPrivateKey = adminPrivateKey;
	}

	public PublicKey getAdminPublicKey()
	{
		return adminPublicKey;
	}

	public void setAdminPublicKey(PublicKey adminPublicKey)
	{
		this.adminPublicKey = adminPublicKey;
	}

	public PrivateKey getPublishingPrivateKey()
	{
		return publishingPrivateKey;
	}

	public void setPublishingPrivateKey(PrivateKey publishingPrivateKey)
	{
		this.publishingPrivateKey = publishingPrivateKey;
	}

	public PublicKey getPublishingPublicKey()
	{
		return publishingPublicKey;
	}

	public void setPublishingPublicKey(PublicKey publishingPublicKey)
	{
		this.publishingPublicKey = publishingPublicKey;
	}

	public byte[] getSignature()
	{
		return signature;
	}

	public void setSignature(byte[] signature)
	{
		this.signature = signature;
	}

	@Override
	public int writeObject(ByteBuf buf, Set<SerializationFlags> flags)
	{
		var size = 0;

		size += serialize(buf, API_VERSION_2); // current RS API
		var sizeOffset = buf.writerIndex();
		size += serialize(buf, 0); // write size at the end
		size += serialize(buf, gxsId, GxsId.class);
		size += serialize(buf, originalGxsId, GxsId.class);
		size += serialize(buf, parentId, GxsId.class);
		size += serialize(buf, TlvType.STRING, name);
		size += serialize(buf, diffusionFlags, FieldSize.INTEGER);
		size += serialize(buf, (int) published.getEpochSecond());
		size += serialize(buf, circleType);
		size += serialize(buf, authenticationFlags);
		size += serialize(buf, author, GxsId.class);
		size += serialize(buf, TlvType.STRING, serviceString);
		size += serialize(buf, circleId, GxsId.class);
		size += serialize(buf, TlvType.SIGNATURE_SET, flags.contains(SerializationFlags.SIGNATURE) ? new SignatureSet() : createSignatureSet());
		size += serialize(buf, TlvType.SECURITY_KEY_SET, createSecurityKeySet());
		size += serialize(buf, signatureFlags, FieldSize.INTEGER);
		buf.setInt(sizeOffset, size); // write total size

		return size;
	}

	@Override
	public void readObject(ByteBuf buf, Set<SerializationFlags> serializationFlags)
	{
		var apiVersion = deserializeInt(buf);
		if (apiVersion != API_VERSION_1 && apiVersion != API_VERSION_2)
		{
			throw new IllegalArgumentException("Unsupported API version " + apiVersion);
		}
		var size = deserializeInt(buf); // XXX: check size (this size is only the meta data size, not the full buffer)
		gxsId = (GxsId) deserializeIdentifier(buf, GxsId.class);
		originalGxsId = (GxsId) deserializeIdentifier(buf, GxsId.class);
		parentId = (GxsId) deserializeIdentifier(buf, GxsId.class);
		name = (String) deserialize(buf, TlvType.STRING);
		diffusionFlags = deserializeEnumSet(buf, GxsPrivacyFlags.class, FieldSize.INTEGER);
		published = Instant.ofEpochSecond(deserializeInt(buf));
		circleType = deserializeEnum(buf, GxsCircleType.class);
		authenticationFlags = deserializeInt(buf);
		author = (GxsId) deserializeIdentifier(buf, GxsId.class);
		serviceString = (String) deserialize(buf, TlvType.STRING);
		circleId = (GxsId) deserializeIdentifier(buf, GxsId.class);
		deserializeSignature(buf);
		deserializeSecurityKeySet(buf);
		if (apiVersion == API_VERSION_2)
		{
			signatureFlags = deserializeEnumSet(buf, GxsSignatureFlags.class, FieldSize.INTEGER);
		}
	}

	/**
	 * Creates a security key set. Note that only public keys are added to it. The private
	 * key has to stay private.
	 *
	 * @return a SecurityKeySet containing public keys
	 */
	private SecurityKeySet createSecurityKeySet()
	{
		var startTs = (int) getPublished().getEpochSecond();
		var stopTs = startTs + 60 * 60 * 24 * 365 * 5; // 5 years

		var securityKeySet = new SecurityKeySet();
		try
		{
			if (adminPublicKey != null)
			{
				securityKeySet.put(new SecurityKey(gxsId, EnumSet.of(SecurityKey.Flags.DISTRIBUTION_ADMIN, SecurityKey.Flags.TYPE_PUBLIC_ONLY), startTs, stopTs, RSA.getPublicKeyAsPkcs1(adminPublicKey)));
			}
			if (publishingPublicKey != null)
			{
				// Identities use a publishing key of type DISTRIBUTION_IDENTITY
				var securityKeyFlag = diffusionFlags.contains(GxsPrivacyFlags.SIGNED_ID) ? SecurityKey.Flags.DISTRIBUTION_IDENTITY : SecurityKey.Flags.DISTRIBUTION_PUBLISHING;

				securityKeySet.put(new SecurityKey(gxsId, EnumSet.of(securityKeyFlag, SecurityKey.Flags.TYPE_PUBLIC_ONLY), startTs, stopTs, RSA.getPublicKeyAsPkcs1(publishingPublicKey)));

			}
		}
		catch (IOException e)
		{
			throw new IllegalArgumentException("Couldn't create RSA key from byte array: " + e.getMessage(), e);
		}
		return securityKeySet;
	}

	private SignatureSet createSignatureSet()
	{
		var signatureSet = new SignatureSet();
		if (getSignature() != null)
		{
			signatureSet.put(SignatureSet.Type.ADMIN, new Signature(gxsId, getSignature()));
		}
		return signatureSet;
	}

	private void deserializeSecurityKeySet(ByteBuf buf)
	{
		var securityKeySet = (SecurityKeySet) deserialize(buf, TlvType.SECURITY_KEY_SET);
		securityKeySet.getPublicKeys().forEach((keyId, securityKey) -> {
			if (securityKey.flags().containsAll(Set.of(SecurityKey.Flags.DISTRIBUTION_ADMIN, SecurityKey.Flags.TYPE_PUBLIC_ONLY)))
			{
				try
				{
					adminPublicKey = RSA.getPublicKeyFromPkcs1(securityKey.data());
				}
				catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e)
				{
					log.error("Identity {} has wrong admin public key: {}", gxsId, e.getMessage());
				}
			}
			else if (securityKey.flags().containsAll(Set.of(SecurityKey.Flags.DISTRIBUTION_IDENTITY, SecurityKey.Flags.TYPE_PUBLIC_ONLY)) ||
					securityKey.flags().containsAll(Set.of(SecurityKey.Flags.DISTRIBUTION_PUBLISHING, SecurityKey.Flags.TYPE_PUBLIC_ONLY)))
			{
				try
				{
					publishingPublicKey = RSA.getPublicKeyFromPkcs1(securityKey.data());
				}
				catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e)
				{
					log.error("Identity {} has wrong publishing public key: {}", gxsId, e.getMessage());
				}
			}
		});
	}

	private void deserializeSignature(ByteBuf buf)
	{
		var signatureSet = (SignatureSet) deserialize(buf, TlvType.SIGNATURE_SET);
		if (signatureSet.getSignatures() != null)
		{
			var sign = signatureSet.getSignatures().get(SignatureSet.Type.ADMIN.getValue());
			if (sign != null)
			{
				signature = sign.data();
			}
		}
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
				", subscribeFlags=" + subscribeFlags +
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
