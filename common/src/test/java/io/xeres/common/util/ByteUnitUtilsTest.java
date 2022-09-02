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

package io.xeres.common.util;

import org.junit.jupiter.api.Test;

import static io.xeres.common.util.ByteUnitUtils.fromBytes;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ByteUnitUtilsTest
{
	@Test
	void ByteUnitUtils_FromBytes_OK()
	{
		assertEquals("invalid", fromBytes(-1));
		assertEquals("0 bytes", fromBytes(0));
		assertEquals("512 bytes", fromBytes(512));
		assertEquals("1023 bytes", fromBytes(1023));
		assertEquals("1 KB", fromBytes(1024));
		assertEquals("1 KB", fromBytes(1152));
		assertEquals("1 MB", fromBytes(1024 * 1024));
		assertEquals("1.12 MB", fromBytes(1152 * 1024));
		assertEquals("1 GB", fromBytes(1024 * 1024 * 1024));
		assertEquals("1 TB", fromBytes(1024L * 1024 * 1024 * 1024));
		assertEquals("1 PB", fromBytes(1024L * 1024 * 1024 * 1024 * 1024));
		assertEquals("???", fromBytes(1024L * 1024 * 1024 * 1024 * 1024 * 1024));
	}
}
