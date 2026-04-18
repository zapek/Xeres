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

package io.xeres.ui.support.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ApplicationExtension.class)
class ImageViewUtilsTest
{
	@Test
	void limitMaximumImageSize_Width_Exceeded()
	{
		var dimension = ImageViewUtils.limitMaximumImageSize(100, 50, 50, 50);
		assertEquals(50, dimension.getWidth());
		assertTrue(dimension.getHeight() < 50);
	}

	@Test
	void limitMaximumImageSize_Height_Exceeded()
	{
		var dimension = ImageViewUtils.limitMaximumImageSize(100, 50, 100, 25);
		assertEquals(25, dimension.getHeight());
		assertTrue(dimension.getHeight() < 100);
	}

	@Test
	void limitMaximumImageSize_WidthAndHeight_Exceeded()
	{
		var dimension = ImageViewUtils.limitMaximumImageSize(800, 600, 320, 240);
		assertTrue(dimension.getWidth() <= 320);
		assertTrue(dimension.getHeight() <= 240);
	}

	@Test
	void limitMaximumImageSize_WidthAndHeight_Exceeded_Different_Ratio()
	{
		var dimension = ImageViewUtils.limitMaximumImageSize(800, 600, 50, 50);
		assertTrue(dimension.getWidth() <= 50);
		assertTrue(dimension.getHeight() <= 50);
	}

	@Test
	void limitMaximumImageSize_Width_Not_Exceeded()
	{
		var dimension = ImageViewUtils.limitMaximumImageSize(100, 50, 100, 50);
		assertEquals(100, dimension.getWidth());
		assertEquals(50, dimension.getHeight());
	}
}