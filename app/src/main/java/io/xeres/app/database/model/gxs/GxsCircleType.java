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

package io.xeres.app.database.model.gxs;

public enum GxsCircleType
{
	UNKNOWN,
	PUBLIC, // not restricted to a circle
	EXTERNAL, // restricted to an external circle
	YOUR_FRIENDS_ONLY, // restricted to a subset of friend nodes
	LOCAL, // not distributed at all
	EXTERNAL_SELF, // self-restricted, not used except at creation time when the circle ID isn't known yet. set to external afterwards
	YOUR_EYES_ONLY // distributed to locations signed by own profile only
}
