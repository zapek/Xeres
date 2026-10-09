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
import io.xeres.ui.controller.common.GxsMessage;

import java.time.Instant;

public class MailMessage implements GxsMessage
{
	@Override
	public long getId()
	{
		return 0;
	}

	@Override
	public GxsId getGxsId()
	{
		return null;
	}

	@Override
	public long getOriginalId()
	{
		return 0;
	}

	@Override
	public Instant getPublished()
	{
		return null;
	}

	@Override
	public boolean isRead()
	{
		return false;
	}

	@Override
	public void setRead(boolean read)
	{

	}
}
