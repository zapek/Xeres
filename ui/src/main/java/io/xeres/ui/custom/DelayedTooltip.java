/*
 * Copyright (c) 2024 by David Gerber - https://zapek.com
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

package io.xeres.ui.custom;

import javafx.scene.control.Tooltip;

import java.util.function.Consumer;

/**
 * A tooltip subclass that allows to generate the string on demand (for example
 * for a network call).
 */
public class DelayedTooltip extends Tooltip
{
	private Consumer<DelayedTooltip> consumer;

	public DelayedTooltip()
	{
		super();
	}

	public DelayedTooltip(String text)
	{
		super(text);
	}

	/**
	 * Creates a DelayedTooltip that will call the consumer when it's about to show.
	 * The consumer has to call {@link #show(String)} to make the tooltip visible.
	 *
	 * @param consumer the consumer
	 */
	public DelayedTooltip(Consumer<DelayedTooltip> consumer)
	{
		this.consumer = consumer;
	}

	@Override
	protected void show()
	{
		if (consumer == null)
		{
			super.show();
		}
		else
		{
			consumer.accept(this);
		}
	}

	public void show(String text)
	{
		consumer = null; // Now show() must really show
		setText(text);
		show();
	}
}
