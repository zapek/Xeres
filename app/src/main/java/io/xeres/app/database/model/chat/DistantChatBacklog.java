/*
 * Copyright (c) 2025 by David Gerber - https://zapek.com
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

package io.xeres.app.database.model.chat;

import io.xeres.app.xrs.service.identity.item.IdentityGroupItem;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
public class DistantChatBacklog
{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "identity_id", nullable = false)
	private IdentityGroupItem identityGroupItem;

	@CreationTimestamp
	private Instant created;

	private boolean own;

	private String message;

	protected DistantChatBacklog()
	{

	}

	public DistantChatBacklog(IdentityGroupItem identityGroupItem, boolean own, String message)
	{
		this.identityGroupItem = identityGroupItem;
		this.own = own;
		this.message = message;
	}

	public long getId()
	{
		return id;
	}

	public void setId(long id)
	{
		this.id = id;
	}

	public IdentityGroupItem getIdentityGroupItem()
	{
		return identityGroupItem;
	}

	public void setIdentityGroupItem(IdentityGroupItem identityGroupItem)
	{
		this.identityGroupItem = identityGroupItem;
	}

	public Instant getCreated()
	{
		return created;
	}

	public void setCreated(Instant created)
	{
		this.created = created;
	}

	public boolean isOwn()
	{
		return own;
	}

	public void setOwn(boolean own)
	{
		this.own = own;
	}

	public String getMessage()
	{
		return message;
	}

	public void setMessage(String message)
	{
		this.message = message;
	}
}
