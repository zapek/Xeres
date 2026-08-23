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

package io.xeres.common.util;

import io.xeres.testutils.TestUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LottieUtilsTest
{
	private static Path tgsFile;
	private static Path lottieFile;
	private static Path jsonFile;
	private static Path pngFile;

	@BeforeAll
	static void setup()
	{
		tgsFile = Path.of("/image/xeres_dance.tgs");
		lottieFile = Path.of("/image/xeres_dance.lottie");
		jsonFile = Path.of("/image/xeres_dance.json");
		pngFile = Path.of("/image/ours.png");
	}

	@Test
	void Instance_ThrowsException() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(LottieUtils.class);
	}

	@Test
	void isLottieSubSet()
	{
		assertTrue(LottieUtils.isLottieSubSet(tgsFile));
		assertTrue(LottieUtils.isLottieSubSet(lottieFile));
		assertTrue(LottieUtils.isLottieSubSet(jsonFile));
		assertFalse(LottieUtils.isLottieSubSet(pngFile));
	}

	@Test
	void isTgsFile()
	{
		assertTrue(LottieUtils.isTgsFile(tgsFile));
		assertTrue(LottieUtils.isTgsFile(Path.of("/image/xeres_dance.TGS")));
		assertFalse(LottieUtils.isTgsFile(pngFile));
	}

	@Test
	void writeLottieData()
	{
		assertEquals("data:" + LottieUtils.TGS_MIMETYPE + ";base64,dGVzdA==",
				LottieUtils.writeLottieData(LottieUtils.TGS_MIMETYPE, "test".getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	void isLottieData()
	{
		assertTrue(LottieUtils.isLottieData("data:" + LottieUtils.LOTTIE_MIMETYPE + ";base64,dGVzdA=="));
		assertTrue(LottieUtils.isLottieData("data:" + LottieUtils.JSON_MIMETYPE + ";base64,dGVzdA=="));
		assertTrue(LottieUtils.isLottieData("data:" + LottieUtils.TGS_MIMETYPE + ";base64,dGVzdA=="));
		assertFalse(LottieUtils.isLottieData("data:image/png;base64,iVBOR"));
		assertFalse(LottieUtils.isLottieData("foobar"));
		assertFalse(LottieUtils.isLottieData(""));
		assertFalse(LottieUtils.isLottieData(null));
		assertFalse(LottieUtils.isLottieData("data:"));
	}

	@Test
	void isMimeType()
	{
		var dataUri = "data:" + LottieUtils.JSON_MIMETYPE + ";base64,dGVzdA==";

		assertTrue(LottieUtils.isMimeType(dataUri, LottieUtils.JSON_MIMETYPE));
		assertFalse(LottieUtils.isMimeType(dataUri, LottieUtils.TGS_MIMETYPE));
		assertFalse(LottieUtils.isMimeType(dataUri, LottieUtils.LOTTIE_MIMETYPE));
		assertFalse(LottieUtils.isMimeType(null, LottieUtils.JSON_MIMETYPE));
		assertFalse(LottieUtils.isMimeType("", LottieUtils.JSON_MIMETYPE));
	}

	@Test
	void isLottieSizeSmallEnough()
	{
		// No limit
		assertTrue(LottieUtils.isLottieSizeSmallEnough(10_000_000, 0));

		// Base64 expansion of 300 bytes is exactly 400 chars
		assertTrue(LottieUtils.isLottieSizeSmallEnough(300, 600));
		assertFalse(LottieUtils.isLottieSizeSmallEnough(301, 600));
		assertFalse(LottieUtils.isLottieSizeSmallEnough(300, 599));
	}

	@Test
	void readLottieData()
	{
		assertArrayEquals("test".getBytes(StandardCharsets.UTF_8),
				LottieUtils.readLottieData("data:" + LottieUtils.TGS_MIMETYPE + ";base64,dGVzdA=="));

		assertArrayEquals(new byte[0], LottieUtils.readLottieData("data:" + LottieUtils.TGS_MIMETYPE + ";base64,not base64!"));
	}
}