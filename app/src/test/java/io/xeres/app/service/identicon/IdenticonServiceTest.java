/*
 * Copyright (c) 2026 by David Gerber - https://zapek.com
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

package io.xeres.app.service.identicon;

import io.xeres.app.configuration.CacheDirConfiguration;
import io.xeres.testutils.IdFakes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

@ExtendWith(MockitoExtension.class)
class IdenticonServiceTest
{
	@SuppressWarnings("unused")
	@Mock
	private CacheDirConfiguration cacheDirConfiguration;

	@InjectMocks
	private IdenticonService identiconService;

	@Test
	void getIdenticon()
	{
		var gxsId = IdFakes.createGxsId(new byte[]{91, 51, 48, 44, 32, 45, 49, 49, 51, 44, 32, 50, 48, 44, 32, 54});

		var expected = new byte[]{-119, 80, 78, 71, 13, 10, 26, 10, 0, 0, 0, 13, 73, 72, 68, 82, 0, 0, 0, -128, 0,
				0, 0, -128, 8, 6, 0, 0, 0, -61, 62, 97, -53, 0, 0, 2, 79, 73, 68, 65, 84, 120, 94, -19, -37, -79,
				-115, -35, 80, 20, 3, -47, 45, -55, -128, -53, 113, -1, -7, -17, -64, -50, 39, -40, -128, -64, 93,
				60, -102, 19, -100, 68, 9, -95, -85, 73, -11, -11, -25, -9, -81, -65, -1, -117, -49, -25, -13, 35,
				-72, -37, -20, -117, 15, -102, -15, 67, 93, -31, 110, 51, 3, 8, 112, -73, -103, 1, 4, -72, -37, -52,
				0, 2, -36, 109, 102, 0, 1, -18, 54, 51, -128, 0, 119, -101, 25, 64, -128, -69, -51, 12, 32, -64, -35,
				102, 6, 16, -32, 110, 51, 3, 8, 112, -73, -103, 1, 4, -72, -37, -52, 0, 2, -36, 109, 102, 0, 1, -18,
				54, 51, -128, 0, 119, -101, 25, 64, -128, -69, -51, 12, 32, -64, -35, 102, 6, 16, -32, 110, 51, 3, 8,
				112, -73, -103, 1, 4, -72, -37, -52, 0, 2, -36, 109, 102, 0, 1, -18, 54, 51, -128, 0, 119, -101, 25,
				64, -128, -69, -51, 12, 32, -64, -35, 102, 6, 16, -32, 110, 51, 3, 8, 112, -73, -103, 1, 4, -72, -37,
				-52, 0, 2, -36, 109, 102, 0, 1, -18, 54, 51, -128, 0, 119, -101, 25, 64, -128, -69, -51, 12, 32, -64,
				-35, 102, 6, 16, -32, 110, 51, 3, 8, 112, -73, -103, 1, 4, -72, -37, -52, 0, 2, -36, 109, 102, 0, 1,
				-18, 54, 51, -128, 0, 119, -101, 25, 64, -128, -69, -51, 12, 32, -64, -35, 102, 6, 16, -32, 110, 51,
				3, 8, 112, -73, -103, 1, 4, -72, -37, -52, 0, 2, -36, 109, 102, 0, 1, -18, 54, 51, -128, 0, 119, -101,
				25, 64, -128, -69, -51, 12, 32, -64, -35, 102, 6, 16, -32, 110, 51, 3, 8, 112, -73, -39, 23, 95, 78,
				91, 12, 96, -100, 1, -116, 51, -128, 113, 6, 48, -50, 0, -58, 25, -64, 56, 3, 24, 103, 0, -29, 12, 96,
				-100, 1, -116, 51, -128, 113, 6, 48, -50, 0, -58, 25, -64, 56, 3, 24, 103, 0, -29, 12, 96, -100, 1,
				-116, 51, -128, 113, 6, 48, -50, 0, -58, 25, -64, 56, 3, 24, 103, 0, -29, 12, 96, -36, -113, -3, 26,
				-58, 97, 125, -113, -9, -69, 98, 0, -113, -30, -3, -82, 24, -64, -93, 120, -65, 43, 6, -16, 40, -34,
				-17, -118, 1, 60, -118, -9, -69, 98, 0, -113, -30, -3, -82, 24, -64, -93, 120, -65, 43, 6, -16, 40,
				-34, -17, -118, 1, 60, -118, -9, -69, 98, 0, -113, -30, -3, -82, 24, -64, -93, 120, -65, 43, 6, -16,
				40, -34, -17, -118, 1, 60, -118, -9, -69, 98, 0, -113, -30, -3, -82, 24, -64, -93, 120, -65, 43, 6,
				-16, 40, -34, -17, -118, 1, 60, -118, -9, -69, 98, 0, -113, -30, -3, -82, 24, -64, -93, 120, -65, 43,
				6, -16, 40, -34, -17, -118, 1, 60, -118, -9, -69, 98, 0, -113, -30, -3, -82, 24, -64, -93, 120, -65,
				43, 6, -16, 40, -34, -17, -118, 1, 60, -118, -9, -69, -14, 99, 1, -24, 77, 6, 48, -50, 0, -58, 25,
				-64, 56, 3, 24, 103, 0, -29, 12, 96, -100, 1, -116, 51, -128, 113, 6, 48, -50, 0, -58, 25, -64, 56,
				3, 24, 103, 0, -29, 12, 96, -100, 1, -116, 51, -128, 113, 6, 48, -50, 0, -58, 25, -64, 56, 3, 24,
				103, 0, -29, 12, 96, -100, 1, -116, 51, -128, 113, 6, 48, -18, 31, 34, -98, 74, 113, 126, 103, -43,
				77, 0, 0, 0, 0, 73, 69, 78, 68, -82, 66, 96, -126};

		var result = identiconService.getIdenticon(gxsId.getBytes());

		assertArrayEquals(expected, result);
	}
}