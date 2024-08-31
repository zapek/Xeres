/*
 * Copyright (c) 2023 by David Gerber - https://zapek.com
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

package io.xeres.app.xrs.common;

import io.xeres.common.id.GxsId;
import io.xeres.common.id.Id;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;

import static io.xeres.app.xrs.common.SecurityKey.Flags.DISTRIBUTION_ADMIN;
import static io.xeres.app.xrs.common.SecurityKey.Flags.TYPE_PUBLIC_ONLY;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SecurityKeyTest
{
	@Test
	void CompareTo_Success()
	{
		var securityKey1 = new SecurityKey(new GxsId(Id.toBytes("11111111111111111111111111111111")), EnumSet.of(TYPE_PUBLIC_ONLY, DISTRIBUTION_ADMIN), 0, 1, new byte[1]);
		var securityKey2 = new SecurityKey(new GxsId(Id.toBytes("22222222222222222222222222222222")), EnumSet.of(TYPE_PUBLIC_ONLY, DISTRIBUTION_ADMIN), 0, 1, new byte[1]);
		var securityKey3 = new SecurityKey(new GxsId(Id.toBytes("33333333333333333333333333333333")), EnumSet.of(TYPE_PUBLIC_ONLY, DISTRIBUTION_ADMIN), 0, 1, new byte[1]);
		var securityKey4 = new SecurityKey(new GxsId(Id.toBytes("44444444444444444444444444444444")), EnumSet.of(TYPE_PUBLIC_ONLY, DISTRIBUTION_ADMIN), 0, 1, new byte[1]);
		var securityKey5 = new SecurityKey(new GxsId(Id.toBytes("55555555555555555555555555555555")), EnumSet.of(TYPE_PUBLIC_ONLY, DISTRIBUTION_ADMIN), 0, 1, new byte[1]);

		var unorderedList = List.of(securityKey3, securityKey1, securityKey4, securityKey2, securityKey5);

		var orderedList = unorderedList.stream()
				.sorted()
				.toList();

		assertEquals(securityKey1, orderedList.get(0));
		assertEquals(securityKey2, orderedList.get(1));
		assertEquals(securityKey3, orderedList.get(2));
		assertEquals(securityKey4, orderedList.get(3));
		assertEquals(securityKey5, orderedList.get(4));
	}
}
