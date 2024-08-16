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

package io.xeres.ui.support.window;

public record WindowBorder(double leftSize, double topSize, double rightSize, double bottomSize)
{
	public static final WindowBorder DEFAULT = new WindowBorder(5.0, 20.0, 5.0, 5.0);

	public boolean isEmpty()
	{
		return leftSize == 0.0 && topSize == 0.0 && rightSize == 0.0 && bottomSize == 0.0 ||
				Double.isNaN(leftSize) && Double.isNaN(topSize) && Double.isNaN(rightSize) && Double.isNaN(bottomSize);
	}

	@Override
	public String toString()
	{
		return "WindowBorder{" +
				"leftSize=" + leftSize +
				", topSize=" + topSize +
				", rightSize=" + rightSize +
				", bottomSize=" + bottomSize +
				'}';
	}
}
