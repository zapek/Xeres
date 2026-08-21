/*
 * Copyright (c) 2025-2026 by David Gerber - https://zapek.com
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

package io.xeres.ui.custom.event;

import io.xeres.ui.custom.sticker.Sticker;
import javafx.event.Event;
import javafx.event.EventType;

import java.io.Serial;

public class StickerSelectedEvent extends Event
{
	@Serial
	private static final long serialVersionUID = -1377318297476370274L;

	public static final EventType<StickerSelectedEvent> STICKER_SELECTED = new EventType<>(ANY, "STICKER_SELECTED");

	private final transient Sticker sticker;
	private final transient StickerType stickerType;

	public enum StickerType
	{
		IMAGE,
		LOTTIE
	}

	public StickerSelectedEvent(Sticker sticker, StickerType stickerType)
	{
		super(STICKER_SELECTED);
		this.sticker = sticker;
		this.stickerType = stickerType;
	}

	public StickerType getStickerType()
	{
		return stickerType;
	}

	public Sticker getSticker()
	{
		return sticker;
	}
}
