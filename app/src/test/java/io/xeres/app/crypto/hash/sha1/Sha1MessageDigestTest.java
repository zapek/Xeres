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

package io.xeres.app.crypto.hash.sha1;

import io.xeres.common.id.Sha1Sum;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class Sha1MessageDigestTest
{
	private static final HexFormat HEX = HexFormat.of();

	@Test
	void update_EmptyInput_ShouldMatchFipsVector()
	{
		var digest = new Sha1MessageDigest();

		digest.update(new byte[0]);

		assertArrayEquals(HEX.parseHex("da39a3ee5e6b4b0d3255bfef95601890afd80709"), digest.getBytes());
	}

	@Test
	void update_Abc_ShouldMatchFipsVector()
	{
		var digest = new Sha1MessageDigest();

		digest.update("abc".getBytes(StandardCharsets.US_ASCII));

		assertArrayEquals(HEX.parseHex("a9993e364706816aba3e25717850c26c9cd0d89d"), digest.getBytes());
	}

	@Test
	void update_LongFipsVector_ShouldMatchFipsVector()
	{
		var digest = new Sha1MessageDigest();

		digest.update("abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq".getBytes(StandardCharsets.US_ASCII));

		assertArrayEquals(HEX.parseHex("84983e441c3bd26ebaae4aa1f95129e5e54670f1"), digest.getBytes());
	}

	@Test
	void update_WithOffsetAndLength_ShouldHashSliceOnly()
	{
		var digest = new Sha1MessageDigest();
		var input = "xxabcxx".getBytes(StandardCharsets.US_ASCII);

		digest.update(input, 2, 3);

		assertArrayEquals(HEX.parseHex("a9993e364706816aba3e25717850c26c9cd0d89d"), digest.getBytes());
	}

	@Test
	void update_WithByteBuffer_ShouldMatchFipsVector()
	{
		var digest = new Sha1MessageDigest();

		digest.update(ByteBuffer.wrap("abc".getBytes(StandardCharsets.US_ASCII)));

		assertArrayEquals(HEX.parseHex("a9993e364706816aba3e25717850c26c9cd0d89d"), digest.getBytes());
	}

	@Test
	void update_Chunked_ShouldMatchSingleUpdate()
	{
		var single = new Sha1MessageDigest();
		single.update("abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq".getBytes(StandardCharsets.US_ASCII));

		var chunked = new Sha1MessageDigest();
		chunked.update("abcdbcdecdefdefg".getBytes(StandardCharsets.US_ASCII));
		chunked.update("efghfghighijhijkijkljklmklmnlmnomnopnopq".getBytes(StandardCharsets.US_ASCII));

		assertArrayEquals(single.getBytes(), chunked.getBytes());
	}

	@Test
	void getBytes_TwiceWithoutUpdate_ShouldReturnSameDigest()
	{
		var digest = new Sha1MessageDigest();

		digest.update("abc".getBytes(StandardCharsets.US_ASCII));

		assertArrayEquals(digest.getBytes(), digest.getBytes());
	}

	@Test
	void getSum_Abc_ShouldReturnSha1Sum()
	{
		var digest = new Sha1MessageDigest();

		digest.update("abc".getBytes(StandardCharsets.US_ASCII));

		assertEquals(new Sha1Sum(HEX.parseHex("a9993e364706816aba3e25717850c26c9cd0d89d")), digest.getSum());
	}
}
