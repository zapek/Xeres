/*
 * Copyright (c) 2019-2026 by David Gerber - https://zapek.com
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
import io.xeres.app.xrs.common.Signature;
import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.serialization.SerializationFlags;
import io.xeres.app.xrs.serialization.TlvType;
import io.xeres.app.xrs.service.gxs.item.DynamicServiceType;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.MsgId;
import jakarta.persistence.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static io.xeres.app.database.model.gxs.GxsConstants.GXS_ITEM_MAX_SIZE;
import static io.xeres.app.xrs.serialization.Serializer.*;

@Entity(name = "gxs_message")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class GxsMessageItem extends Item implements GxsMetaAndData, DynamicServiceType
{
	private static final Logger log = LoggerFactory.getLogger(GxsMessageItem.class);

	private static final int API_VERSION_1 = 0x0000;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Embedded
	@AttributeOverride(name = "identifier", column = @Column(name = "gxs_id"))
	private GxsId gxsId;
	@Embedded
	@AttributeOverride(name = "identifier", column = @Column(name = "message_id"))
	private MsgId msgId;
	@Embedded
	@AttributeOverride(name = "identifier", column = @Column(name = "thread_id"))
	private MsgId threadMsgId; // Used for comments and votes (attaches them to a message)
	@Embedded
	@AttributeOverride(name = "identifier", column = @Column(name = "parent_id"))
	private MsgId parentMsgId;
	@Embedded
	@AttributeOverride(name = "identifier", column = @Column(name = "original_message_id"))
	private MsgId originalMsgId;
	@Embedded
	@AttributeOverride(name = "identifier", column = @Column(name = "author_id"))
	private GxsId authorGxsId;

	private String name;

	private Instant published; // publishts (32-bits)

	// Lower 16-bits are available for services, higher is reserved. Forums and wiki use it.
	private int flags;

	// Local storage only, sets the message as hidden because it was superseded by another message (edited)
	private boolean hidden;

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

	public MsgId getMsgId()
	{
		return msgId;
	}

	public void setMsgId(MsgId msgId)
	{
		this.msgId = msgId;
	}

	public MsgId getOriginalMsgId()
	{
		return msgId.equals(originalMsgId) ? null : originalMsgId; // Shouldn't happen anymore but older versions might have saved the message with the format used by RS
	}

	public void setOriginalMsgId(MsgId originalMsgId)
	{
		this.originalMsgId = originalMsgId;
	}

	public MsgId getParentMsgId()
	{
		return parentMsgId;
	}

	public void setParentMsgId(MsgId parentMsgId)
	{
		this.parentMsgId = parentMsgId;
	}

	public boolean isChild()
	{
		return parentMsgId != null;
	}

	public GxsId getAuthorGxsId()
	{
		return authorGxsId;
	}

	public void setAuthorGxsId(GxsId authorGxsId)
	{
		this.authorGxsId = authorGxsId;
	}

	public boolean hasAuthor()
	{
		return authorGxsId != null;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public Instant getPublished()
	{
		return published;
	}

	public void updatePublished()
	{
		published = Instant.now();
	}

	public boolean isHidden()
	{
		return hidden;
	}

	public void setHidden(boolean hidden)
	{
		this.hidden = hidden;
	}

	public byte[] getPublishSignature()
	{
		return signatures.stream()
				.filter(signature -> signature.getType() == Signature.Type.PUBLISH)
				.findFirst().orElseThrow().getData();
	}

	public void setPublishSignature(byte[] publishSignature)
	{
		signatures.stream()
				.filter(signature -> signature.getType() == Signature.Type.PUBLISH)
				.findFirst().ifPresent(signatures::remove); // XXX: hack! This is caused because it shouldn't be a set to begin with!
		var signature = new Signature(Signature.Type.PUBLISH, gxsId, publishSignature);
		signatures.add(signature);
	}

	public byte[] getAuthorSignature()
	{
		return signatures.stream()
				.filter(signature -> signature.getType() == Signature.Type.AUTHOR)
				.findFirst().orElseThrow().getData();
	}

	public void setAuthorSignature(byte[] authorSignature)
	{
		Objects.requireNonNull(authorGxsId);
		signatures.stream()
				.filter(signature -> signature.getType() == Signature.Type.AUTHOR)
				.findFirst().ifPresent(signatures::remove); // XXX: hack! This is caused because it shouldn't be a set to begin with!
		var signature = new Signature(Signature.Type.AUTHOR, authorGxsId, authorSignature);
		signatures.add(signature);
	}

	@Override
	public int writeMetaObject(ByteBuf buf, Set<SerializationFlags> serializationFlags)
	{
		var size = 0;

		size += serialize(buf, API_VERSION_1); // API version
		var sizeOffset = buf.writerIndex();
		size += serialize(buf, 0); // write size at the end
		size += serialize(buf, gxsId, GxsId.class);
		size += serialize(buf, serializationFlags.contains(SerializationFlags.SIGNATURE) ? null : msgId, MsgId.class);
		size += serialize(buf, threadMsgId, MsgId.class);
		size += serialize(buf, parentMsgId, MsgId.class);
		size += serialize(buf, serializationFlags.contains(SerializationFlags.SIGNATURE) && Objects.equals(msgId, originalMsgId) ? null : originalMsgId, MsgId.class);
		size += serialize(buf, authorGxsId, GxsId.class);
		size += serialize(buf, TlvType.SIGNATURE_SET, serializationFlags.contains(SerializationFlags.SIGNATURE) ? new HashSet<>() : signatures);
		size += serialize(buf, TlvType.STR_NONE, name);
		size += serialize(buf, (int) published.getEpochSecond());
		size += serialize(buf, flags);
		buf.setInt(sizeOffset, size); // write total size

		return size;
	}

	@Override
	public void readMetaObject(ByteBuf buf)
	{
		var apiVersion = deserializeInt(buf);
		if (apiVersion != API_VERSION_1)
		{
			throw new IllegalArgumentException("Unsupported API version " + apiVersion);
		}
		var size = deserializeInt(buf); // the size
		if (size > GXS_ITEM_MAX_SIZE)
		{
			throw new IllegalArgumentException("Gxs message meta size " + size + " is bigger than the maximum of " + GXS_ITEM_MAX_SIZE);
		}
		gxsId = (GxsId) deserializeIdentifier(buf, GxsId.class);
		msgId = (MsgId) deserializeIdentifier(buf, MsgId.class);
		threadMsgId = (MsgId) deserializeIdentifier(buf, MsgId.class);
		parentMsgId = (MsgId) deserializeIdentifier(buf, MsgId.class);
		originalMsgId = (MsgId) deserializeIdentifier(buf, MsgId.class);
		if (msgId.equals(originalMsgId))
		{
			// RS does this weird thing, we get rid of it and use null instead.
			originalMsgId = null;
		}
		authorGxsId = (GxsId) deserializeIdentifier(buf, GxsId.class);
		deserializeSignature(buf);
		name = (String) deserialize(buf, TlvType.STR_NONE);
		published = Instant.ofEpochSecond(deserializeInt(buf));
		flags = deserializeInt(buf);
	}

	private void deserializeSignature(ByteBuf buf)
	{
		@SuppressWarnings("unchecked") var signatureSet = (Set<Signature>) deserialize(buf, TlvType.SIGNATURE_SET);
		signatures.clear();
		signatureSet.forEach(signature -> {
			if (signature.getType() == Signature.Type.PUBLISH || signature.getType() == Signature.Type.AUTHOR)
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
	public GxsMessageItem clone()
	{
		return (GxsMessageItem) super.clone();
	}

	@Override
	public String toString()
	{
		return "id=" + id +
				", gxsId=" + gxsId +
				", msgId=" + msgId +
				", name='" + StringUtils.truncate(name, 64) + '\'' +
				", threadMsgId=" + threadMsgId +
				", parentMsgId=" + parentMsgId +
				", originalMsgId=" + originalMsgId +
				", authorGxsId=" + authorGxsId +
				", published=" + published +
				", flags=" + flags +
				", hidden=" + hidden;
	}
}
