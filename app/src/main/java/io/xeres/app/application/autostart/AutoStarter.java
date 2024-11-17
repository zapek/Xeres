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

package io.xeres.app.application.autostart;

public interface AutoStarter
{
	/**
	 * Checks if the auto start feature is supported by the system.
	 * <p>
	 * Usually depends on the host OS and installation mode (for example, portable mode doesn't support auto start).
	 *
	 * @return true if auto start is supported
	 */
	boolean isSupported();

	/**
	 * Checks if the auto start feature is enabled for the application.
	 *
	 * @return true if auto start is enabled
	 */
	boolean isEnabled();

	/**
	 * Enables auto start for the application.
	 */
	void enable();

	/**
	 * Disables auto start for the application.
	 */
	void disable();
}
