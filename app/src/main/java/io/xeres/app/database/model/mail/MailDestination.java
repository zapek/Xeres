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

import io.xeres.common.id.GxsId;
import io.xeres.common.id.LocationIdentifier;
import io.xeres.common.mail.DeliveryType;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
public class MailDestination
{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "mail_message_id", nullable = false)
	private MailMessage mailMessage;

	private DeliveryType type;

	private Instant lastSent = Instant.EPOCH;

	private byte[] destination;

	public MailDestination()
	{
	}

	public MailDestination(MailMessage mailMessage, LocationIdentifier locationIdentifier)
	{
		this.mailMessage = mailMessage;
		type = DeliveryType.LOCATION;
		destination = locationIdentifier.getBytes();
	}

	public MailDestination(MailMessage mailMessage, GxsId gxsId)
	{
		this.mailMessage = mailMessage;
		type = DeliveryType.DISTANT;
		destination = gxsId.getBytes();
	}

	public long getId()
	{
		return id;
	}

	public void setId(long id)
	{
		this.id = id;
	}

	public MailMessage getMailMessage()
	{
		return mailMessage;
	}

	public void setMailMessage(MailMessage mailMessage)
	{
		this.mailMessage = mailMessage;
	}

	public DeliveryType getType()
	{
		return type;
	}

	public void setType(DeliveryType type)
	{
		this.type = type;
	}

	public Instant getLastSent()
	{
		return lastSent;
	}

	public void setLastSent(Instant lastSent)
	{
		this.lastSent = lastSent;
	}

	public byte[] getDestination()
	{
		return destination;
	}

	public void setDestination(byte[] destination)
	{
		this.destination = destination;
	}
}
