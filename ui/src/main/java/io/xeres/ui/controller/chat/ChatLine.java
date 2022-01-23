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

package io.xeres.ui.controller.chat;

import javafx.scene.image.Image;

import java.time.Instant;

public class ChatLine
{
	private final Instant instant;
	private final String action;
	private final String message;
	private final Image image;

	public ChatLine(Instant instant, String action, String message, Image image)
	{
		this.instant = instant;
		this.action = action;
		this.message = message;
		this.image = image;
	}

	public Instant getInstant()
	{
		return instant;
	}

	public String getAction()
	{
		return action;
	}

	public String getMessage()
	{
		return message;
	}

	public Image getImage()
	{
		if (image != null && !image.isError())
		{
			return image;
		}
		return null;
	}
}
