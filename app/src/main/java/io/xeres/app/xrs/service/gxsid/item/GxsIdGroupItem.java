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

package io.xeres.app.xrs.service.gxsid.item;

import io.netty.buffer.ByteBuf;
import io.xeres.app.database.model.gxs.GxsGroupItem;
import io.xeres.app.xrs.serialization.RsSerializable;
import io.xeres.app.xrs.serialization.SerializationFlags;
import io.xeres.app.xrs.serialization.TlvType;
import io.xeres.app.xrs.service.RsServiceType;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.Sha1Sum;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static io.xeres.app.xrs.serialization.Serializer.*;

@Entity(name = "gxs_id_groups")
public class GxsIdGroupItem extends GxsGroupItem implements RsSerializable // XXX: beware because we need to be able to serialize just the group data (here) and the group metadata (superclass)
{
	@Embedded
	@AttributeOverride(name = "identifier", column = @Column(name = "profile_hash"))
	private Sha1Sum profileHash; // hash of the gxsId + public key
	private byte[] profileSignature; // XXX: warning, RS puts this in a string! we might have to do some serialization trickery... see p3idservice.cc in service_createGroup(), but I think my system's flexibility makes up for it

	@Transient
	private List<String> recognitionTags = new ArrayList<>(); // not used (but serialized)

	// XXX: add avatar image (TlvImage)... the avatar image is optional so we can just ignore it for now
	// and it checks if an image is here by checking the size... sigh! will have to use custom serialization

	public GxsIdGroupItem()
	{
	}

	public GxsIdGroupItem(GxsId gxsId, String name)
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
		this.profileSignature = profileSignature;
	}

	@Override
	public int writeObject(ByteBuf buf, Set<SerializationFlags> serializationFlags)
	{
		var size = 0;

		if (serializationFlags.contains(SerializationFlags.SUBCLASS_ONLY))
		{
			size += writeObject(buf);
		}
		else if (serializationFlags.contains(SerializationFlags.SUPERCLASS_ONLY))
		{
			return super.writeObject(buf, serializationFlags);
		}
		else
		{
			size += super.writeObject(buf, serializationFlags);
			size += writeObject(buf);
		}
		return size;
	}

	private int writeObject(ByteBuf buf)
	{
		var size = 0;

		size += serialize(buf, (byte) 2);
		size += serialize(buf, (short) RsServiceType.GXSID.getType());
		size += serialize(buf, (byte) 2);
		var sizeOffset = buf.writerIndex();
		size += serialize(buf, 0); // write size at end

		size += serialize(buf, profileHash, Sha1Sum.class);
		size += serialize(buf, TlvType.STR_SIGN, profileSignature);
		size += serialize(buf, TlvType.SET_RECOGN, recognitionTags);

		// XXX: missing avatar image here

		buf.setInt(sizeOffset, size); // write total size

		return size;
	}

	@Override
	public void readObject(ByteBuf buf, Set<SerializationFlags> serializationFlags)
	{
		if (serializationFlags.contains(SerializationFlags.SUBCLASS_ONLY))
		{
			readObject(buf);
		}
		else if (serializationFlags.contains(SerializationFlags.SUPERCLASS_ONLY))
		{
			super.readObject(buf, serializationFlags);
		}
		else
		{
			super.readObject(buf, serializationFlags);
			readObject(buf);
		}
	}

	@SuppressWarnings("unchecked")
	private void readObject(ByteBuf buf)
	{
		// XXX: we have to read the following but... shouldn't there be something else to do it?
		buf.readByte(); // 0x2 (packet version)
		buf.readShort(); // 0x0211 (service: gxsId)
		buf.readByte(); // 0x2 (packet subtype?)
		buf.readInt(); // size

		profileHash = (Sha1Sum) deserializeIdentifier(buf, Sha1Sum.class);
		profileSignature = (byte[]) deserialize(buf, TlvType.STR_SIGN);
		recognitionTags = (List<String>) deserialize(buf, TlvType.SET_RECOGN);

		if (buf.isReadable()) // XXX: I think this works if there's more data to read
		{
			// XXX: read the avatar image, which is a RsTlvImage (type: 0x1060), like: 1060 00000010 00000000013000000006
			buf.discardReadBytes(); // XXX!
		}
	}
}
