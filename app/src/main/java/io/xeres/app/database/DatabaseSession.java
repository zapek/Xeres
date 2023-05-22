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

package io.xeres.app.database;

/**
 * Allows using transactions from outside spring controllers, while still allowing the controller
 * to call such methods directly. For example:
 * {@snippet :
 *     @Autowired
 *     private DatabaseSessionManager databaseSessionManager;
 *     try (var session = new DatabaseSession(databaseSessionManager))
 *     {
 *         // use your JPA entity here
 *     }
 *}
 */
public class DatabaseSession implements AutoCloseable
{
	private final DatabaseSessionManager databaseSessionManager;
	private final boolean isBound;

	public DatabaseSession(DatabaseSessionManager databaseSessionManager)
	{
		this.databaseSessionManager = databaseSessionManager;
		isBound = databaseSessionManager.bindSession();
	}

	@Override
	public void close()
	{
		if (isBound)
		{
			databaseSessionManager.unbindSession();
		}
	}
}
