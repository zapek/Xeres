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

package io.xeres.app.database.model.gxs;

import io.netty.buffer.ByteBuf;
import io.xeres.app.xrs.common.Signature;
import io.xeres.app.xrs.common.SignatureSet;
import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.serialization.SerializationFlags;
import io.xeres.app.xrs.serialization.TlvType;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.MessageId;
import jakarta.persistence.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Set;

import static io.xeres.app.xrs.serialization.Serializer.*;

@Entity(name = "gxs_messages")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class GxsMessageItem extends Item implements GxsMetaAndData
{
	private static final Logger log = LoggerFactory.getLogger(GxsMessageItem.class);

	private static final int API_VERSION_2 = 0x0000;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Embedded
	@AttributeOverride(name = "identifier", column = @Column(name = "gxs_id"))
	private GxsId gxsId;
	@Embedded
	@AttributeOverride(name = "identifier", column = @Column(name = "message_id"))
	private MessageId messageId;
	@Embedded
	@AttributeOverride(name = "identifier", column = @Column(name = "thread_id"))
	private MessageId threadId;
	@Embedded
	@AttributeOverride(name = "identifier", column = @Column(name = "parent_id"))
	private MessageId parentId;
	@Embedded
	@AttributeOverride(name = "identifier", column = @Column(name = "original_message_id"))
	private MessageId originalMessageId;
	@Embedded
	@AttributeOverride(name = "identifier", column = @Column(name = "author_id"))
	private GxsId authorId;

	private String name;

	private Instant published; // publishts (32-bits)

	//private Set<GxsMessageFlags> messageFlags; .. use a converter, etc... right now it seems there's only RS_GXS_FORUM_MSG_FLAGS_MODERATED
	// msgflags (32-bits). use serialize(buf, msgFlags, FieldSize.INTEGER) ... or maybe just serialize the integer as the bits are user defined...
	private int flags;

	private int status; // see GXS_MSG_STATUS_*

	private Instant child;

	private String serviceString;

	@Transient
	private byte[] publishSignature;

	@Transient
	private byte[] identitySignature;

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

	public MessageId getMessageId()
	{
		return messageId;
	}

	public void setMessageId(MessageId messageId)
	{
		this.messageId = messageId;
	}

	public GxsId getAuthorId()
	{
		return authorId;
	}

	public void setAuthorId(GxsId authorId)
	{
		this.authorId = authorId;
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

	public byte[] getPublishSignature()
	{
		return publishSignature;
	}

	public void setPublishSignature(byte[] publishSignature)
	{
		this.publishSignature = publishSignature;
	}

	public byte[] getIdentitySignature()
	{
		return identitySignature;
	}

	public void setIdentitySignature(byte[] identitySignature)
	{
		this.identitySignature = identitySignature;
	}

	@Override
	public int writeMetaObject(ByteBuf buf, Set<SerializationFlags> serializationFlags)
	{
		var size = 0;

		size += serialize(buf, API_VERSION_2);
		var sizeOffset = buf.writerIndex();
		size += serialize(buf, 0); // write size at the end
		size += serialize(buf, gxsId, GxsId.class);
		size += serialize(buf, messageId, MessageId.class);
		size += serialize(buf, threadId, MessageId.class);
		size += serialize(buf, parentId, MessageId.class);
		size += serialize(buf, originalMessageId, MessageId.class);
		size += serialize(buf, authorId, GxsId.class);
		size += serialize(buf, TlvType.SIGNATURE_SET, serializationFlags.contains(SerializationFlags.SIGNATURE) ? new SignatureSet() : createSignatureSet());
		size += serialize(buf, TlvType.STRING, name);
		size += serialize(buf, (int) published.getEpochSecond());
		size += serialize(buf, flags); // XXX: or diffusionFlags/messageFlags, FieldSize.INTEGER, see how groups does it
		buf.setInt(sizeOffset, size); // write total size

		return size;
	}

	@Override
	public void readMetaObject(ByteBuf buf)
	{
		var apiVersion = deserializeInt(buf);
		if (apiVersion != API_VERSION_2)
		{
			throw new IllegalArgumentException("Unsupported API version " + apiVersion);
		}
		deserializeInt(buf); // the size
		gxsId = (GxsId) deserializeIdentifier(buf, GxsId.class);
		messageId = (MessageId) deserializeIdentifier(buf, MessageId.class);
		threadId = (MessageId) deserializeIdentifier(buf, MessageId.class);
		parentId = (MessageId) deserializeIdentifier(buf, MessageId.class);
		originalMessageId = (MessageId) deserializeIdentifier(buf, MessageId.class);
		authorId = (GxsId) deserializeIdentifier(buf, GxsId.class);
		deserializeSignature(buf);
		name = (String) deserialize(buf, TlvType.STRING);
		published = Instant.ofEpochSecond(deserializeInt(buf));
		flags = deserializeInt(buf); // XXX: or use enumset, etc...
	}

	private SignatureSet createSignatureSet()
	{
		var signatureSet = new SignatureSet();
		if (getPublishSignature() != null)
		{
			signatureSet.put(SignatureSet.Type.PUBLISH, new Signature(gxsId, getPublishSignature()));
		}
		if (getIdentitySignature() != null)
		{
			signatureSet.put(SignatureSet.Type.IDENTITY, new Signature(gxsId, getIdentitySignature()));
		}
		return signatureSet;
	}

	private void deserializeSignature(ByteBuf buf)
	{
		var signatureSet = (SignatureSet) deserialize(buf, TlvType.SIGNATURE_SET);
		if (signatureSet.getSignatures() != null)
		{
			var sign = signatureSet.getSignatures().get(SignatureSet.Type.PUBLISH.getValue());
			if (sign != null)
			{
				publishSignature = sign.data();
			}
			sign = signatureSet.getSignatures().get(SignatureSet.Type.IDENTITY.getValue());
			if (sign != null)
			{
				identitySignature = sign.data();
			}
		}
	}

	@Override
	public String toString()
	{
		return "GxsMessageItem{" +
				"id=" + id +
				", gxsId=" + gxsId +
				", messageId=" + messageId +
				", threadId=" + threadId +
				", parentId=" + parentId +
				", originalMessageId=" + originalMessageId +
				", authorId=" + authorId +
				", name='" + name + '\'' +
				", published=" + published +
				", flags=" + flags +
				", status=" + status +
				", child=" + child +
				", serviceString='" + serviceString + '\'' +
				'}';
	}
}
