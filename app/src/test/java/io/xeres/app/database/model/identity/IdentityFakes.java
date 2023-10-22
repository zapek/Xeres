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

package io.xeres.app.database.model.identity;

import io.xeres.app.xrs.service.identity.item.IdentityGroupItem;
import io.xeres.common.identity.Type;
import io.xeres.testutils.IdFakes;
import io.xeres.testutils.StringFakes;

import static io.xeres.common.dto.identity.IdentityConstants.OWN_IDENTITY_ID;

public final class IdentityFakes
{
	private IdentityFakes()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	private static long id = OWN_IDENTITY_ID + 1;

	private static long getUniqueId()
	{
		return id++;
	}

	public static IdentityGroupItem createOwn()
	{
		return createOwn(StringFakes.createNickname());
	}

	public static IdentityGroupItem createOwn(String name)
	{
		var identity = new IdentityGroupItem(IdFakes.createGxsId(), name);
		identity.setId(1L);
		identity.setType(Type.OWN);
		return identity;
	}
}
