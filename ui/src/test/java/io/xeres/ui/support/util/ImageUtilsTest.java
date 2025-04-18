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
import io.xeres.ui.support.util.image.ImageUtils;
import javafx.scene.image.Image;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageUtilsTest
{
	private static Image image;
	private static BufferedImage bufferedImage;

	@BeforeAll
	static void setup() throws IOException
	{
		image = new Image(Objects.requireNonNull(ImageUtilsTest.class.getResourceAsStream("/image/ours.png")));
		bufferedImage = ImageIO.read(Objects.requireNonNull(ImageUtilsTest.class.getResourceAsStream("/image/ours.png")));
	}

	@Test
	void Instance_ThrowsException() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(ImageUtils.class);
	}

	@Test
	void WriteImageAsPngData_Image_Success()
	{
		var pngImage = ImageUtils.writeImageAsPngData(image, 2048);

		assertTrue(pngImage.startsWith("data:image/png;base64,iVBOR"), pngImage);
	}

	@Test
	void WriteImageAsPngData_BufferedImage_Success()
	{
		var pngImage = ImageUtils.writeImageAsPngData(bufferedImage, 2048);

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

	@Test
	void LimitMaximumImageSize_Success()
	{
		var scaledImage = ImageUtils.limitMaximumImageSize(bufferedImage, 128);

		assertTrue(scaledImage.getWidth() * scaledImage.getHeight() <= 128);
	}
}
