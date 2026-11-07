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

package io.xeres.ui.model.mail;

import io.xeres.common.id.GxsId;
import io.xeres.common.id.LocationIdentifier;
import io.xeres.common.mail.MailType;
import io.xeres.ui.controller.common.GxsMessage;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

public class MailMessage implements GxsMessage
{
	private long id;
	private MailType type;
	private Instant sent;
	private Instant published;
	private String authorName;
	private LocationIdentifier locationFrom;
	private GxsId identityFrom;
	private final Set<LocationIdentifier> locationsTo = new HashSet<>();
	private final Set<LocationIdentifier> locationsCc = new HashSet<>();
	private final Set<LocationIdentifier> locationsBcc = new HashSet<>();
	private final Set<GxsId> identitiesTo = new HashSet<>();
	private final Set<GxsId> identitiesCc = new HashSet<>();
	private final Set<GxsId> identitiesBcc = new HashSet<>();
	private String subject;
	private String content;
	private boolean read;
	private boolean starred;
	//private List<> files = new ArrayList<>(); XXX

	@Override
	public long getId()
	{
		return id;
	}

	public void setId(long id)
	{
		this.id = id;
	}

	@Override
	public GxsId getGxsId()
	{
		return type.getGxsId();
	}

	@Override
	public long getOriginalId()
	{
		return 0;
	}

	@Override
	public Instant getPublished()
	{
		return published;
	}

	public void setPublished(Instant published)
	{
		this.published = published;
	}

	@Override
	public boolean isRead()
	{
		return read;
	}

	@Override
	public void setRead(boolean read)
	{
		this.read = read;
	}

	public MailType getType()
	{
		return type;
	}

	public void setType(MailType type)
	{
		this.type = type;
	}

	public Instant getSent()
	{
		return sent;
	}

	public void setSent(Instant sent)
	{
		this.sent = sent;
	}

	public String getAuthorName()
	{
		return authorName;
	}

	public void setAuthorName(String authorName)
	{
		this.authorName = authorName;
	}

	public LocationIdentifier getLocationFrom()
	{
		return locationFrom;
	}

	public void setLocationFrom(LocationIdentifier locationFrom)
	{
		this.locationFrom = locationFrom;
	}

	public GxsId getIdentityFrom()
	{
		return identityFrom;
	}

	public void setIdentityFrom(GxsId identityFrom)
	{
		this.identityFrom = identityFrom;
	}

	public Set<LocationIdentifier> getLocationsTo()
	{
		return locationsTo;
	}

	public void addLocationsTo(Set<LocationIdentifier> locationsTo)
	{
		this.locationsTo.addAll(locationsTo);
	}


	public Set<LocationIdentifier> getLocationsCc()
	{
		return locationsCc;
	}

	public void addLocationsCc(Set<LocationIdentifier> locationsCc)
	{
		this.locationsCc.addAll(locationsCc);
	}

	public Set<LocationIdentifier> getLocationsBcc()
	{
		return locationsBcc;
	}

	public void addLocationsBcc(Set<LocationIdentifier> locationsBcc)
	{
		this.locationsBcc.addAll(locationsBcc);
	}

	public Set<GxsId> getIdentitiesTo()
	{
		return identitiesTo;
	}

	public void addIdentitiesTo(Set<GxsId> identitiesTo)
	{
		this.identitiesTo.addAll(identitiesTo);
	}

	public Set<GxsId> getIdentitiesCc()
	{
		return identitiesCc;
	}

	public void addIdentitiesCc(Set<GxsId> identitiesCc)
	{
		this.identitiesCc.addAll(identitiesCc);
	}

	public Set<GxsId> getIdentitiesBcc()
	{
		return identitiesBcc;
	}

	public void addIdentitiesBcc(Set<GxsId> identitiesBcc)
	{
		this.identitiesBcc.addAll(identitiesBcc);
	}

	public String getSubject()
	{
		return subject;
	}

	public void setSubject(String subject)
	{
		this.subject = subject;
	}

	public String getContent()
	{
		return content;
	}

	public void setContent(String content)
	{
		this.content = content;
	}

	public boolean isStarred()
	{
		return starred;
	}

	public void setStarred(boolean starred)
	{
		this.starred = starred;
	}
}
