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
import io.xeres.app.database.converter.GxsPrivacyFlagsConverter;
import io.xeres.app.database.converter.GxsSignatureFlagsConverter;
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

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

import static io.xeres.app.xrs.serialization.Serializer.*;

@Entity(name = "gxs_groups")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class GxsGroupItem extends Item implements RsSerializable
{
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

	private byte[] adminPrivateKeyData;
	private byte[] adminPublicKeyData;

	// the publishing key is used for both DISTRIBUTION_PUBLISHING and DISTRIBUTION_IDENTITY
	private byte[] publishingPrivateKeyData;
	private byte[] publishingPublicKeyData;

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

	public byte[] getAdminPrivateKeyData()
	{
		return adminPrivateKeyData;
	}

	public void setAdminPrivateKeyData(byte[] adminPrivateKeyData)
	{
		this.adminPrivateKeyData = adminPrivateKeyData;
	}

	public byte[] getAdminPublicKeyData()
	{
		return adminPublicKeyData;
	}

	public void setAdminPublicKeyData(byte[] adminPublicKeyData)
	{
		this.adminPublicKeyData = adminPublicKeyData;
	}

	public byte[] getPublishingPrivateKeyData()
	{
		return publishingPrivateKeyData;
	}

	public void setPublishingPrivateKeyData(byte[] publishingPrivateKeyData)
	{
		this.publishingPrivateKeyData = publishingPrivateKeyData;
	}

	public byte[] getPublishingPublicKeyData()
	{
		return publishingPublicKeyData;
	}

	public void setPublishingPublicKeyData(byte[] publishingPublicKeyData)
	{
		this.publishingPublicKeyData = publishingPublicKeyData;
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
		int size = 0;

		size += serialize(buf, 0xaf01); // current RS API (XXX: put that constant somewhere)
		int sizeOffset = buf.writerIndex();
		size += serialize(buf, 0); // write size at the end
		size += serialize(buf, gxsId);
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
		int apiVersion = deserializeInt(buf);
		if (apiVersion != 0xaf01)
		{
			throw new IllegalArgumentException("Unsupported API version " + apiVersion);
		}
		int size = deserializeInt(buf); // XXX: check size (this size is only the meta data size, not the full buffer)
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
		deserializePublicKeys(buf);
		signatureFlags = deserializeEnumSet(buf, GxsSignatureFlags.class, FieldSize.INTEGER);
	}

	/**
	 * Creates a security key set. Note that only public keys are added to it. The private
	 * key has to stay private.
	 *
	 * @return a SecurityKeySet containing public keys
	 */
	private SecurityKeySet createSecurityKeySet()
	{
		int startTs = (int) getPublished().getEpochSecond();
		int stopTs = startTs + 60 * 60 * 24 * 365 * 5; // 5 years

		var securityKeySet = new SecurityKeySet();
		if (adminPublicKeyData != null)
		{
			securityKeySet.put(new SecurityKey(gxsId, EnumSet.of(SecurityKey.Flags.DISTRIBUTION_ADMIN, SecurityKey.Flags.TYPE_PUBLIC_ONLY), startTs, stopTs, adminPublicKeyData));
		}
		if (publishingPublicKeyData != null)
		{
			// Identities use a publishing key of type DISTRIBUTION_IDENTITY
			if (diffusionFlags.contains(GxsPrivacyFlags.SIGNED_ID))
			{
				securityKeySet.put(new SecurityKey(gxsId, EnumSet.of(SecurityKey.Flags.DISTRIBUTION_IDENTITY, SecurityKey.Flags.TYPE_PUBLIC_ONLY), startTs, stopTs, publishingPublicKeyData));
			}
			else
			{
				securityKeySet.put(new SecurityKey(gxsId, EnumSet.of(SecurityKey.Flags.DISTRIBUTION_PUBLISHING, SecurityKey.Flags.TYPE_PUBLIC_ONLY), startTs, stopTs, publishingPublicKeyData));
			}
		}
		return securityKeySet;
	}

	private SignatureSet createSignatureSet()
	{
		Objects.requireNonNull(getSignature());
		var signatureSet = new SignatureSet();
		signatureSet.put(SignatureSet.Type.ADMIN, new Signature(gxsId, getSignature()));
		return signatureSet;
	}

	private void deserializePublicKeys(ByteBuf buf)
	{
		var securityKeySet = (SecurityKeySet) deserialize(buf, TlvType.SECURITY_KEY_SET);
		securityKeySet.getPublicKeys().forEach((keyId, securityKey) -> {
			if (securityKey.getFlags().containsAll(Set.of(SecurityKey.Flags.DISTRIBUTION_ADMIN, SecurityKey.Flags.TYPE_PUBLIC_ONLY)))
			{
				adminPublicKeyData = securityKey.getData();
			}
			else if (securityKey.getFlags().containsAll(Set.of(SecurityKey.Flags.DISTRIBUTION_IDENTITY, SecurityKey.Flags.TYPE_PUBLIC_ONLY)) ||
					securityKey.getFlags().containsAll(Set.of(SecurityKey.Flags.DISTRIBUTION_PUBLISHING, SecurityKey.Flags.TYPE_PUBLIC_ONLY)))
			{
				publishingPublicKeyData = securityKey.getData();
			}
		});
	}

	private void deserializeSignature(ByteBuf buf)
	{
		var signatureSet = (SignatureSet) deserialize(buf, TlvType.SIGNATURE_SET);
		if (signatureSet.getSignatures() != null)
		{
			signature = signatureSet.getSignatures().get(SignatureSet.Type.ADMIN.getValue()).getData();
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
