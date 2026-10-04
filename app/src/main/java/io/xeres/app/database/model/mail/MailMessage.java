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

package io.xeres.app.database.model.mail;

import io.xeres.app.xrs.common.FileItem;
import io.xeres.app.xrs.common.FileSet;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.LocationIdentifier;
import io.xeres.common.mail.MailType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
public class MailMessage
{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	private MailType type;

	private Instant sent;

	@CreationTimestamp
	private Instant published; // The name is important (used by the ArgumentResolver)

	@Embedded
	@AttributeOverride(name = "identifier", column = @Column(name = "LOCATION_FROM"))
	private LocationIdentifier locationFrom;

	@Embedded
	@AttributeOverride(name = "identifier", column = @Column(name = "IDENTITY_FROM"))
	private GxsId identityFrom;

	@ElementCollection
	@CollectionTable(name = "MAIL_MESSAGE_LOCATION_TO", joinColumns = @JoinColumn(name = "MAIL_MESSAGE_ID"))
	private final Set<LocationIdentifier> locationsTo = new HashSet<>();

	@ElementCollection
	@CollectionTable(name = "MAIL_MESSAGE_LOCATION_CC", joinColumns = @JoinColumn(name = "MAIL_MESSAGE_ID"))
	private final Set<LocationIdentifier> locationsCc = new HashSet<>();

	@ElementCollection
	@CollectionTable(name = "MAIL_MESSAGE_LOCATION_BCC", joinColumns = @JoinColumn(name = "MAIL_MESSAGE_ID"))
	private final Set<LocationIdentifier> locationsBcc = new HashSet<>();

	@ElementCollection
	@CollectionTable(name = "MAIL_MESSAGE_IDENTITY_TO", joinColumns = @JoinColumn(name = "MAIL_MESSAGE_ID"))
	private final Set<GxsId> identitiesTo = new HashSet<>();

	@ElementCollection
	@CollectionTable(name = "MAIL_MESSAGE_IDENTITY_CC", joinColumns = @JoinColumn(name = "MAIL_MESSAGE_ID"))
	private final Set<GxsId> identitiesCc = new HashSet<>();

	@ElementCollection
	@CollectionTable(name = "MAIL_MESSAGE_IDENTITY_BCC", joinColumns = @JoinColumn(name = "MAIL_MESSAGE_ID"))
	private final Set<GxsId> identitiesBcc = new HashSet<>();

	// XXX: seems the limit is 2 MB for both total! attachements are just links anyway..
	private String subject;

	private String content;

	private boolean read;

	private boolean starred;

	@ElementCollection
	private List<FileItem> files = new ArrayList<>();

	public MailMessage()
	{
	}

	public MailMessage(MailType type, Instant sent, LocationIdentifier from, String subject, String content, Set<LocationIdentifier> to, FileSet files)
	{
		this.type = type;
		this.sent = sent;
		locationFrom = from;
		this.subject = subject;
		this.content = content;
		locationsTo.addAll(to);
		this.files.addAll(files.fileItems());
	}

	public MailMessage(MailType type, Instant sent, GxsId from, String subject, String content, Set<GxsId> to, FileSet files)
	{
		this.type = type;
		this.sent = sent;
		identityFrom = from;
		this.subject = subject;
		this.content = content;
		identitiesTo.addAll(to);
		this.files.addAll(files.fileItems());
	}

	public void addTo(LocationIdentifier to)
	{
		locationsTo.add(to);
	}

	public void addCc(LocationIdentifier cc)
	{
		locationsCc.add(cc);
	}

	public void addBcc(LocationIdentifier bcc)
	{
		locationsBcc.add(bcc);
	}

	public void addTo(GxsId to)
	{
		identitiesTo.add(to);
	}

	public void addCc(GxsId cc)
	{
		identitiesCc.add(cc);
	}

	public void addBcc(GxsId bcc)
	{
		identitiesBcc.add(bcc);
	}

	public boolean isDirect()
	{
		return locationFrom != null;
	}

	public long getId()
	{
		return id;
	}

	public LocationIdentifier getLocationFrom()
	{
		return locationFrom;
	}

	public GxsId getIdentityFrom()
	{
		return identityFrom;
	}

	public Set<LocationIdentifier> getLocationsTo()
	{
		return locationsTo;
	}

	public Set<LocationIdentifier> getLocationsCc()
	{
		return locationsCc;
	}

	public Set<LocationIdentifier> getLocationsBcc()
	{
		return locationsBcc;
	}

	public Set<GxsId> getIdentitiesTo()
	{
		return identitiesTo;
	}

	public Set<GxsId> getIdentitiesCc()
	{
		return identitiesCc;
	}

	public Set<GxsId> getIdentitiesBcc()
	{
		return identitiesBcc;
	}

	public Instant getPublished()
	{
		return published;
	}

	public String getSubject()
	{
		return subject;
	}

	public String getContent()
	{
		return content;
	}

	public boolean isRead()
	{
		return read;
	}

	public boolean isStarred()
	{
		return starred;
	}

	public List<FileItem> getFiles()
	{
		return files;
	}
}
