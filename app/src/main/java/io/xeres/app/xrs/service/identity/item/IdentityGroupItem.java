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

package io.xeres.app.xrs.service.identity.item;

import io.netty.buffer.ByteBuf;
import io.xeres.app.database.converter.IdentityTypeConverter;
import io.xeres.app.database.model.gxs.GxsGroupItem;
import io.xeres.app.xrs.serialization.SerializationFlags;
import io.xeres.app.xrs.serialization.TlvType;
import io.xeres.app.xrs.service.RsServiceType;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.Sha1Sum;
import io.xeres.common.identity.Type;
import jakarta.persistence.*;
import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static io.xeres.app.xrs.serialization.Serializer.*;

@Entity(name = "identity_groups")
public class IdentityGroupItem extends GxsGroupItem // XXX: beware because we need to be able to serialize just the group data (here) and the group metadata (superclass)
{
	@Embedded
	@AttributeOverride(name = "identifier", column = @Column(name = "profile_hash"))
	private Sha1Sum profileHash; // hash of the gxsId + public key
	private byte[] profileSignature; // XXX: warning, RS puts this in a string! we might have to do some serialization trickery... see p3idservice.cc in service_createGroup(), but I think my system's flexibility makes up for it

	@Transient
	private List<String> recognitionTags = new ArrayList<>(); // not used (but serialized)

	private byte[] image;
	@Transient
	private Boolean hasImage;

	@Convert(converter = IdentityTypeConverter.class)
	private Type type = Type.OTHER;

	public IdentityGroupItem()
	{
	}

	public IdentityGroupItem(GxsId gxsId, String name)
	{
		setGxsId(gxsId);
		setName(name);
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

	public boolean hasImage()
	{
		return hasImage != null ? hasImage : image != null;
	}

	public void setHasImage(boolean value)
	{
		this.hasImage = value;
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
	public int writeGroupObject(ByteBuf buf, Set<SerializationFlags> serializationFlags)
	{
		var size = 0;

		size += serialize(buf, profileHash, Sha1Sum.class);
		size += serialize(buf, TlvType.STR_SIGN, profileSignature);
		size += serialize(buf, TlvType.SET_RECOGN, recognitionTags);
		if (image != null)
		{
			size += serialize(buf, TlvType.IMAGE, image);
		}
		return size;
	}

	@Override
	public void readGroupObject(ByteBuf buf)
	{
		profileHash = (Sha1Sum) deserializeIdentifier(buf, Sha1Sum.class);
		setProfileSignature((byte[]) deserialize(buf, TlvType.STR_SIGN));
		//noinspection unchecked
		recognitionTags = (List<String>) deserialize(buf, TlvType.SET_RECOGN);

		if (buf.isReadable())
		{
			setImage((byte[]) deserialize(buf, TlvType.IMAGE));
		}
	}

	@Override
	public RsServiceType getServiceType()
	{
		return RsServiceType.GXSID;
	}
}
