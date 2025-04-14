/*
 * Copyright (c) 2019-2025 by David Gerber - https://zapek.com
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

import io.xeres.testutils.TestUtils;
import javafx.scene.image.Image;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageUtilsTest
{
	private static Image image;

	@BeforeAll
	static void setup()
	{
		image = new Image(Objects.requireNonNull(ImageUtilsTest.class.getResourceAsStream("/image/ours.png")));
	}

	@Test
	void Instance_ThrowsException() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(ImageUtils.class);
	}

	@Test
	void WriteImageAsPngData_Success()
	{
		var pngImage = ImageUtils.writeImageAsPngData(image, 8192);

		assertTrue(pngImage.startsWith("data:image/png;base64,iVBOR"), pngImage);
	}

	@Test
	void WriteImageAsJpegData_Success()
	{
		var jpegImage = ImageUtils.writeImageAsJpegData(image, 2048);

		assertTrue(jpegImage.startsWith("data:image/jpeg;base64,/9j/"), jpegImage);
	}

	@Test
	void WriteImageAsJpegDataWithLimit_Success()
	{
		var jpegImage = ImageUtils.writeImageAsJpegData(image, 256);

		assertTrue(jpegImage.startsWith("data:image/jpeg;base64,/9j/"), jpegImage);
	}
}
