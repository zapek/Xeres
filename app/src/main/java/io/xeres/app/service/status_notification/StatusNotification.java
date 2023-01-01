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

package io.xeres.app.service.status_notification;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Objects;

class StatusNotification<T>
{
	private T value;
	private boolean changed;

	public StatusNotification(T value)
	{
		this.value = value;
	}

	public T getValue()
	{
		return value;
	}

	public void setValue(T value)
	{
		if (!Objects.equals(this.value, value))
		{
			changed = true;
			this.value = value;
		}
	}

	public boolean isChanged()
	{
		return changed;
	}

	public T getNewStatusIfChanged(SseEmitter sseEmitter)
	{
		if (isChanged() || sseEmitter != null)
		{
			if (sseEmitter == null)
			{
				changed = false;
			}
			return value;
		}
		return null;
	}
}
