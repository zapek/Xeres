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

package io.xeres.common.util.image;

import io.xeres.testutils.TestUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageUtilsTest
{
	private static BufferedImage opaqueImage;
	private static BufferedImage transparentImage;

	@BeforeAll
	static void setup() throws IOException
	{
		opaqueImage = ImageIO.read(Objects.requireNonNull(ImageUtilsTest.class.getResourceAsStream("/image/ours.png")));
		transparentImage = ImageIO.read(Objects.requireNonNull(ImageUtilsTest.class.getResourceAsStream("/image/logo_transparent.png")));
	}

	@Test
	void Instance_ThrowsException() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(ImageUtils.class);
	}

	@Test
	void WriteImageAsPngData_Image_Success()
	{
		var pngImage = ImageUtils.writeImageAsPngData(ImageUtilsTest.opaqueImage, 2048);

		assertTrue(pngImage.startsWith("data:image/png;base64,iVBOR"));
	}

	@Test
	void WriteImageAsJpegData_Success()
	{
		var jpegImage = ImageUtils.writeImageAsJpegData(opaqueImage, 2048);

		assertTrue(jpegImage.startsWith("data:image/jpeg;base64,/9j/"));
	}

	@Test
	void WriteImageAsJpegDataWithLimit_Success()
	{
		var jpegImage = ImageUtils.writeImageAsJpegData(opaqueImage, 256);

		assertTrue(jpegImage.startsWith("data:image/jpeg;base64,/9j/"));
	}

	@Test
	void WriteImageAsBestPossibleWhichIsJpeg_Success()
	{
		var bestImage = ImageUtils.writeImage(opaqueImage, 2048);

		assertTrue(bestImage.startsWith("data:image/jpeg;base64,/9j/"));
	}

	@Test
	void WriteImageAsBestPossibleWhichIsPng_Success()
	{
		// This image is effectively transparent and must be written as so.
		var bestImage = ImageUtils.writeImage(transparentImage, 2048);

		assertTrue(bestImage.startsWith("data:image/png;base64,iVBOR"));
	}

	@Test
	void LimitMaximumImageSize_Success()
	{
		var scaledImage = ImageUtils.limitMaximumImageSize(opaqueImage, 128);

		assertTrue(scaledImage.getWidth() * scaledImage.getHeight() <= 128);
	}

	@Test
	void DetectJpeg_Success() throws IOException
	{
		var jpegArray = Objects.requireNonNull(ImageUtilsTest.class.getResourceAsStream("/image/hamster.jpg")).readAllBytes();
		assertEquals(MediaType.IMAGE_JPEG, ImageUtils.getImageMimeType(jpegArray));
	}

	@Test
	void DetectPng_Success() throws IOException
	{
		var pngArray = Objects.requireNonNull(ImageUtilsTest.class.getResourceAsStream("/image/ours.png")).readAllBytes();
		assertEquals(MediaType.IMAGE_PNG, ImageUtils.getImageMimeType(pngArray));
	}

	@Test
	void DetectGif_Success() throws IOException
	{
		var gifArray = Objects.requireNonNull(ImageUtilsTest.class.getResourceAsStream("/image/v3_anim.gif")).readAllBytes();
		assertEquals(MediaType.IMAGE_GIF, ImageUtils.getImageMimeType(gifArray));
	}

	@Test
	void DetectWebP_Success() throws IOException
	{
		var webpArray = Objects.requireNonNull(ImageUtilsTest.class.getResourceAsStream("/image/gaudie.webp")).readAllBytes();
		assertEquals(MediaType.parseMediaType("image/webp"), ImageUtils.getImageMimeType(webpArray));
	}
}
