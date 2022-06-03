/*
 * Copyright (c) 2019-2021 by David Gerber - https://zapek.com
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

package io.xeres.app.xrs.service.gxs.item;

import java.util.EnumSet;
import java.util.Set;

public enum TransactionFlags
{
	// States
	START, // FLAG_BEGIN_P1
	START_ACKNOWLEDGE, // FLAG_BEGIN_P2
	END_SUCCESS, // FLAG_END_SUCCESS
	CANCEL, // FLAG_CANCEL (not used it seems)
	END_FAIL_NUM, // FLAG_END_FAIL_NUM (not used it seems)
	END_FAIL_TIMEOUT, // FLAG_END_FAIL_TIMEOUT (not used it seems)
	END_FAIL_FULL, // FLAG_END_FAIL_FULL (not used it seems)
	UNUSED,
	// Types
	TYPE_GROUP_LIST_RESPONSE, // FLAG_TYPE_GRP_LIST_RESP
	TYPE_MESSAGE_LIST_RESPONSE, // FLAG_TYPE_MSG_LIST_RESP
	TYPE_GROUP_LIST_REQUEST, // FLAG_TYPE_GRP_LIST_REQ
	TYPE_MESSAGE_LIST_REQUEST, // FLAG_TYPE_MSG_LIST_REQ
	TYPE_GROUPS, // FLAG_TYPE_GRPS
	TYPE_MESSAGES, // FLAG_TYPE_MESSAGES
	TYPE_ENCRYPTED_DATA; // FLAG_TYPE_ENCRYPTED_DATA (not used it seems)

	public static Set<TransactionFlags> ofStates()
	{
		return EnumSet.of(
				START,
				START_ACKNOWLEDGE,
				END_SUCCESS,
				CANCEL,
				END_FAIL_NUM,
				END_FAIL_TIMEOUT,
				END_FAIL_FULL
		);
	}

	public static Set<TransactionFlags> ofTypes()
	{
		return EnumSet.of(
				TYPE_GROUP_LIST_RESPONSE,
				TYPE_MESSAGE_LIST_RESPONSE,
				TYPE_GROUP_LIST_REQUEST,
				TYPE_MESSAGE_LIST_REQUEST,
				TYPE_GROUPS,
				TYPE_MESSAGES,
				TYPE_ENCRYPTED_DATA);
	}
}
