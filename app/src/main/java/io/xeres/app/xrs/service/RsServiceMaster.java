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

package io.xeres.app.xrs.service;

/**
 * This interface allows to implement dependencies between services, that is, one master service
 * has a list of clients that it can handle. Each master service or client needs to implement this interface
 * which can of course be extended.
 *
 * @see RsServiceSlave
 */
public interface RsServiceMaster<T>
{
	/**
	 * Adds a slave service to a master service. The master service is responsible to handle them.
	 *
	 * @param slave the slave service to add to the master
	 */
	void addRsSlave(T slave);
}
