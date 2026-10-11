/*
 * Copyright (c) 2026 by David Gerber - https://zapek.com
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

package io.xeres.app.xrs.service.mail.item;

import io.xeres.app.xrs.common.FileSet;
import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.item.ItemPriority;
import io.xeres.app.xrs.serialization.RsSerialized;
import io.xeres.app.xrs.serialization.TlvType;
import io.xeres.app.xrs.service.mail.MailFlags;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.Identifier;
import io.xeres.common.id.LocationIdentifier;
import io.xeres.common.protocol.xrs.RsServiceType;
import org.apache.commons.collections4.CollectionUtils;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class MailItem extends Item
{
	@RsSerialized
	private Set<MailFlags> flags;

	@RsSerialized
	private Instant sent;

	@RsSerialized
	private Instant received; // Not used

	@RsSerialized(tlvType = TlvType.STR_SUBJECT)
	private String subject;

	@RsSerialized(tlvType = TlvType.STR_MSG)
	private String message;

	@RsSerialized(tlvType = TlvType.SET_LOCATION_ID)
	private List<LocationIdentifier> locationsTo;

	@RsSerialized(tlvType = TlvType.SET_LOCATION_ID)
	private List<LocationIdentifier> locationsCc;

	@RsSerialized(tlvType = TlvType.SET_LOCATION_ID)
	private List<LocationIdentifier> locationsBcc;

	@RsSerialized(tlvType = TlvType.SET_GXS_ID)
	private List<GxsId> identitiesTo;

	@RsSerialized(tlvType = TlvType.SET_GXS_ID)
	private List<GxsId> identitiesCc;

	@RsSerialized(tlvType = TlvType.SET_GXS_ID)
	private List<GxsId> identitiesBcc; // This doesn't make any sense but RS has it anyway

	@RsSerialized(tlvType = TlvType.FILE_SET)
	private FileSet attachments;

	@SuppressWarnings("unused")
	public MailItem()
	{
	}

	@SuppressWarnings("unchecked")
	public MailItem(Set<? extends Identifier> to, Set<? extends Identifier> cc, String subject, String message, FileSet attachments)
	{
		if (CollectionUtils.isEmpty(to))
		{
			throw new IllegalArgumentException("Missing 'to' for MailItem");
		}
		var firstTo = to.stream().findFirst().orElseThrow();
		if (firstTo instanceof GxsId)
		{
			flags = EnumSet.of(MailFlags.DISTANT);
			identitiesTo = ((Set<GxsId>) to).stream().toList();
			identitiesCc = ((Set<GxsId>) cc).stream().toList();
		}
		else if (firstTo instanceof LocationIdentifier)
		{
			flags = EnumSet.noneOf(MailFlags.class);
			locationsTo = ((Set<LocationIdentifier>) to).stream().toList();
			locationsCc = ((Set<LocationIdentifier>) cc).stream().toList();
		}
		else
		{
			throw new IllegalArgumentException("Unsupported 'to' field for MailItem");
		}
		sent = Instant.now();
		this.subject = subject;
		this.message = message;
		this.attachments = attachments;
	}

	@Override
	public int getServiceType()
	{
		return RsServiceType.MESSAGES.getType();
	}

	@Override
	public int getSubType()
	{
		return 1;
	}

	@Override
	public int getPriority()
	{
		return ItemPriority.BACKGROUND.getPriority();
	}

	public boolean isPartial()
	{
		return flags.contains(MailFlags.PARTIAL);
	}

	public Set<MailFlags> getFlags()
	{
		return flags;
	}

	public Instant getSent()
	{
		return sent;
	}

	public List<LocationIdentifier> getLocationsTo()
	{
		return locationsTo;
	}

	public List<LocationIdentifier> getLocationsCc()
	{
		return locationsCc;
	}

	public List<GxsId> getIdentitiesTo()
	{
		return identitiesTo;
	}

	public List<GxsId> getIdentitiesCc()
	{
		return identitiesCc;
	}

	public String getSubject()
	{
		return subject;
	}

	public String getMessage()
	{
		return message;
	}

	public FileSet getAttachments()
	{
		return attachments;
	}

	@Override
	public MailItem clone()
	{
		return (MailItem) super.clone();
	}

	@Override
	public String toString()
	{
		return "MailItem{" +
				"flags=" + flags +
				", sent=" + sent +
				", received=" + received +
				", subject='" + subject + '\'' +
				'}';
	}
}
