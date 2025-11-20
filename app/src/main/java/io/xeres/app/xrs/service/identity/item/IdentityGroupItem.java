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

package io.xeres.app.xrs.service.identity.item;

import io.netty.buffer.ByteBuf;
import io.xeres.app.database.model.gxs.GxsGroupItem;
import io.xeres.app.database.model.profile.Profile;
import io.xeres.app.xrs.serialization.SerializationFlags;
import io.xeres.app.xrs.serialization.TlvType;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.Sha1Sum;
import io.xeres.common.identity.Type;
import io.xeres.common.util.ByteUnitUtils;
import jakarta.persistence.*;
import org.apache.commons.lang3.ArrayUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static io.xeres.app.xrs.serialization.Serializer.*;

@Entity(name = "identity_group")
public class IdentityGroupItem extends GxsGroupItem
{
	@Transient
	public static final IdentityGroupItem EMPTY = new IdentityGroupItem();

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "profile_id")
	private Profile profile;

	@Embedded
	@AttributeOverride(name = "identifier", column = @Column(name = "profile_hash"))
	private Sha1Sum profileHash; // hash of the gxsId + public key
	private byte[] profileSignature;

	private Instant nextValidation;

	@Transient
	private List<String> recognitionTags = new ArrayList<>(); // not used (but serialized)

	private byte[] image;

	private Type type = Type.OTHER;

	@Transient
	private boolean oldVersion; // Needed because RS added image later, and it would break signature verification otherwise

	public IdentityGroupItem()
	{
	}

	public IdentityGroupItem(GxsId gxsId, String name) // XXX: remove?
	{
		setGxsId(gxsId);
		setName(name);
		updatePublished();
	}

	@Override
	public int getSubType()
	{
		return 2;
	}

	public Profile getProfile()
	{
		return profile;
	}

	public void setProfile(Profile profile)
	{
		this.profile = profile;
	}

	public Sha1Sum getProfileHash()
	{
		return profileHash;
	}

	public void setProfileHash(Sha1Sum profileHash)
	{
		this.profileHash = profileHash;
	}

	public byte[] getProfileSignature()
	{
		return profileSignature;
	}

	public void setProfileSignature(byte[] profileSignature)
	{
		this.profileSignature = ArrayUtils.isNotEmpty(profileSignature) ? profileSignature : null;
	}

	public Instant getNextValidation()
	{
		return nextValidation;
	}

	public void setNextValidation(Instant nextValidation)
	{
		this.nextValidation = nextValidation;
	}

	public boolean hasImage()
	{
		return image != null;
	}

	public byte[] getImage()
	{
		return image;
	}

	public void setImage(byte[] image)
	{
		if (ArrayUtils.isNotEmpty(image))
		{
			this.image = image;
		}
		else
		{
			this.image = null;
		}
	}

	public Type getType()
	{
		return type;
	}

	public void setType(Type type)
	{
		this.type = type;
	}

	@Override
	public int writeDataObject(ByteBuf buf, Set<SerializationFlags> serializationFlags)
	{
		var size = 0;

		size += serialize(buf, profileHash, Sha1Sum.class);
		size += serialize(buf, TlvType.STR_SIGN, profileSignature);
		size += serialize(buf, TlvType.SET_RECOGN, recognitionTags);
		if (!oldVersion)
		{
			size += serialize(buf, TlvType.IMAGE, image);
		}
		return size;
	}

	@Override
	public void readDataObject(ByteBuf buf)
	{
		profileHash = (Sha1Sum) deserializeIdentifier(buf, Sha1Sum.class);
		setProfileSignature((byte[]) deserialize(buf, TlvType.STR_SIGN));
		//noinspection unchecked
		recognitionTags = (List<String>) deserialize(buf, TlvType.SET_RECOGN);

		if (buf.isReadable())
		{
			setImage((byte[]) deserialize(buf, TlvType.IMAGE));
		}
		else
		{
			oldVersion = true;
		}
	}

	@Override
	public IdentityGroupItem clone()
	{
		return (IdentityGroupItem) super.clone();
	}

	@Override
	public String toString()
	{
		return "IdentityGroupItem{" +
				"profile=" + profile +
				", profileHash=" + profileHash +
				", profileSignature=" + (profileSignature != null ? ("yes, " + ByteUnitUtils.fromBytes(profileSignature.length)) : "no") +
				", nextValidation=" + nextValidation +
				", recognitionTags=" + recognitionTags +
				", image=" + (image != null ? ("yes, " + ByteUnitUtils.fromBytes(image.length)) : "no") +
				", type=" + type +
				", oldVersion=" + oldVersion +
				'}';
	}
}
