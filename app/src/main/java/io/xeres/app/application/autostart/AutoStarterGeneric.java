/*
 * Copyright (c) 2019-2022 by David Gerber - https://zapek.com
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

package io.xeres.app.application.autostart;

public class AutoStarterGeneric implements AutoStarter
{
	@Override
	public boolean isSupported()
	{
		return false;
	}

	@Override
	public boolean isEnabled()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void enable()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void disable()
	{
		throw new UnsupportedOperationException();
	}
}
