/*
 * Copyright (c) 2019-2026 by David Gerber - https://zapek.com
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

package io.xeres.app.database.model.gxs;

public enum GxsCircleType
{
	/**
	 * Uninitialized value. For example, Identities are left at that state.
	 */
	UNKNOWN,

	/**
	 * Public distribution. Not restricted to a circle.
	 */
	PUBLIC,

	/**
	 * Restricted to an external circle, based on GxsIds.
	 */
	EXTERNAL,

	/**
	 * Restricted to a group of friend nodes. The administrator of the circle behaves as a controlling hub
	 * for them. Based on PGP ids.
	 */
	YOUR_FRIENDS_ONLY,

	/**
	 * Not distributed at all.
	 */
	LOCAL,

	/**
	 * Self-restricted. Used only at creation time of self-restricted circles, when the
	 * circle ID isn't known yet. Once the circle ID is known, the type
	 * is set to EXTERNAL, and the external circle ID is set to the ID of the circle itself.
	 * Based on GxsIds.
	 */
	EXTERNAL_SELF,

	/**
	 * Distributed to locations signed by own profile only.
	 */
	YOUR_EYES_ONLY
}
