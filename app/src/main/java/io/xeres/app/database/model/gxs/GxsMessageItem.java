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

import io.xeres.app.xrs.item.Item;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.MessageId;
import org.hibernate.annotations.UpdateTimestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import java.time.Instant;

@Entity(name = "gxs_messages")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class GxsMessageItem extends Item
{
	private static final Logger log = LoggerFactory.getLogger(GxsMessageItem.class);

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

	// signSet (RsTlvKeySignatureSet). same as GxsGroupItem signatures... not sure which types we need

	private String name; // tlv string message name (use serialize(buf, TlvType.STRING, name);

	@UpdateTimestamp
	private Instant published; // publishts (32-bits)

	//private Set<GxsMessageFlags> messageFlags; .. use a converter, etc... right now it seems there's only RS_GXS_FORUM_MSG_FLAGS_MODERATED
	// msgflags (32-bits). use serialize(buf, msgFlags, FieldSize.INTEGER) ... or maybe just serialize the integer as the bits are user defined...
	private int flags;

	private int status;

	private Instant child;

	private String serviceString;
}
