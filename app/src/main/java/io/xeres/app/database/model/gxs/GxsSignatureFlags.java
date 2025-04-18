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

public enum GxsSignatureFlags
{
	ENCRYPTED, // unused?
	ALL_SIGNED, // unused?
	THREAD_HEAD, // unused?
	NONE_REQUIRED, // set for all services but never checked
	UNUSED_1,
	UNUSED_2,
	UNUSED_3,
	UNUSED_4,
	ANTI_SPAM,
	AUTHENTICATION_REQUIRED, // unused?
	IF_NO_PUB_SIGN, // unused
	TRACK_MESSAGES, // unused
	ANTI_SPAM_2
}
