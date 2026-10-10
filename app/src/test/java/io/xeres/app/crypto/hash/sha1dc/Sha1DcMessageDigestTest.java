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

package io.xeres.app.crypto.hash.sha1dc;

import io.xeres.app.crypto.hash.sha1.Sha1MessageDigest;
import io.xeres.common.id.Sha1Sum;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class Sha1DcMessageDigestTest
{
	private static final HexFormat HEX = HexFormat.of();

	/// Colliding files found on [sha1collider]()https://github.com/nneonneo/sha1collider/)
	private static final byte[] COLLIDING_HASH = HEX.parseHex("d2651eaebc3b5e460bebef86d8e5b88b3293e785");

	@Test
	void update_EmptyInput_ShouldMatchFipsVector()
	{
		var digest = new Sha1DcMessageDigest();

		digest.update(new byte[0]);

		assertArrayEquals(HEX.parseHex("da39a3ee5e6b4b0d3255bfef95601890afd80709"), digest.getBytes());
	}

	@Test
	void update_Abc_ShouldMatchFipsVector()
	{
		var digest = new Sha1DcMessageDigest();

		digest.update("abc".getBytes(StandardCharsets.US_ASCII));

		assertArrayEquals(HEX.parseHex("a9993e364706816aba3e25717850c26c9cd0d89d"), digest.getBytes());
	}

	@Test
	void update_LongFipsVector_ShouldMatchFipsVector()
	{
		var digest = new Sha1DcMessageDigest();

		digest.update("abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq".getBytes(StandardCharsets.US_ASCII));

		assertArrayEquals(HEX.parseHex("84983e441c3bd26ebaae4aa1f95129e5e54670f1"), digest.getBytes());
	}

	@Test
	void update_WithOffsetAndLength_ShouldHashSliceOnly()
	{
		var digest = new Sha1DcMessageDigest();
		var input = "xxabcxx".getBytes(StandardCharsets.US_ASCII);

		digest.update(input, 2, 3);

		assertArrayEquals(HEX.parseHex("a9993e364706816aba3e25717850c26c9cd0d89d"), digest.getBytes());
	}

	@Test
	void update_WithByteBuffer_ShouldMatchFipsVector()
	{
		var digest = new Sha1DcMessageDigest();

		digest.update(ByteBuffer.wrap("abc".getBytes(StandardCharsets.US_ASCII)));

		assertArrayEquals(HEX.parseHex("a9993e364706816aba3e25717850c26c9cd0d89d"), digest.getBytes());
	}

	@Test
	void update_Chunked_ShouldMatchSingleUpdate()
	{
		var single = new Sha1DcMessageDigest();
		single.update("abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq".getBytes(StandardCharsets.US_ASCII));

		var chunked = new Sha1DcMessageDigest();
		chunked.update("abcdbcdecdefdefg".getBytes(StandardCharsets.US_ASCII));
		chunked.update("efghfghighijhijkijkljklmklmnlmnomnopnopq".getBytes(StandardCharsets.US_ASCII));

		assertArrayEquals(single.getBytes(), chunked.getBytes());
	}

	@Test
	void getBytes_TwiceWithoutUpdate_ShouldReturnSameDigest()
	{
		var digest = new Sha1DcMessageDigest();

		digest.update("abc".getBytes(StandardCharsets.US_ASCII));

		assertArrayEquals(digest.getBytes(), digest.getBytes());
	}

	@Test
	void update_AfterGetBytes_ShouldRestartDigest()
	{
		var digest = new Sha1DcMessageDigest();

		digest.update("abc".getBytes(StandardCharsets.US_ASCII));

		assertArrayEquals(HEX.parseHex("a9993e364706816aba3e25717850c26c9cd0d89d"), digest.getBytes());

		digest.update("abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq".getBytes(StandardCharsets.US_ASCII));

		assertArrayEquals(HEX.parseHex("84983e441c3bd26ebaae4aa1f95129e5e54670f1"), digest.getBytes());
		assertFalse(digest.hasCollision());
	}

	@Test
	void getSum_Abc_ShouldReturnSha1Sum()
	{
		var digest = new Sha1DcMessageDigest();

		digest.update("abc".getBytes(StandardCharsets.US_ASCII));

		assertEquals(new Sha1Sum(HEX.parseHex("a9993e364706816aba3e25717850c26c9cd0d89d")), digest.getSum());
	}

	@Test
	void hasCollision_Abc_ShouldBeFalse()
	{
		var digest = new Sha1DcMessageDigest();

		digest.update("abc".getBytes(StandardCharsets.US_ASCII));
		digest.getBytes();

		assertFalse(digest.hasCollision());
	}

	@Test
	void plainSha1_BothShatteredPdfs_ShouldCollide() throws IOException
	{
		var first = readShatteredPdf("out-eve100.pdf");
		var second = readShatteredPdf("out-eve1b.pdf");

		assertFalse(Arrays.equals(first, second));

		var firstDigest = new Sha1MessageDigest();
		firstDigest.update(first);

		var secondDigest = new Sha1MessageDigest();
		secondDigest.update(second);

		assertArrayEquals(COLLIDING_HASH, firstDigest.getBytes());
		assertArrayEquals(COLLIDING_HASH, secondDigest.getBytes());
	}

	@Test
	void sha1Dc_BothShatteredPdfs_ShouldDetectCollision() throws IOException
	{
		var firstDigest = new Sha1DcMessageDigest();
		firstDigest.update(readShatteredPdf("out-eve100.pdf"));
		firstDigest.getBytes();

		var secondDigest = new Sha1DcMessageDigest();
		secondDigest.update(readShatteredPdf("out-eve1b.pdf"));
		secondDigest.getBytes();

		assertTrue(firstDigest.hasCollision());
		assertTrue(secondDigest.hasCollision());
	}

	@Test
	void sha1Dc_BothShatteredPdfs_ShouldProduceDifferentSafeHashes() throws IOException
	{
		var firstDigest = new Sha1DcMessageDigest();
		firstDigest.update(readShatteredPdf("out-eve100.pdf"));

		var secondDigest = new Sha1DcMessageDigest();
		secondDigest.update(readShatteredPdf("out-eve1b.pdf"));

		var firstSafeHash = firstDigest.getBytes();
		var secondSafeHash = secondDigest.getBytes();

		assertFalse(Arrays.equals(firstSafeHash, secondSafeHash));
		assertFalse(Arrays.equals(firstSafeHash, COLLIDING_HASH));
		assertFalse(Arrays.equals(secondSafeHash, COLLIDING_HASH));
	}

	@Test
	void sha1Dc_ShatteredPdf_ShouldBeDeterministic() throws IOException
	{
		var input = readShatteredPdf("out-eve100.pdf");

		var firstDigest = new Sha1DcMessageDigest();
		firstDigest.update(input);

		var secondDigest = new Sha1DcMessageDigest();
		secondDigest.update(input.clone());

		assertArrayEquals(firstDigest.getBytes(), secondDigest.getBytes());
	}

	@Test
	void sha1Dc_ChunkedShatteredPdf_ShouldMatchSingleUpdate() throws IOException
	{
		var input = readShatteredPdf("out-eve1b.pdf");

		var single = new Sha1DcMessageDigest();
		single.update(input);

		var chunked = new Sha1DcMessageDigest();
		for (int offset = 0; offset < input.length; offset += 7)
		{
			var length = Math.min(7, input.length - offset);
			chunked.update(input, offset, length);
		}

		assertArrayEquals(single.getBytes(), chunked.getBytes());
		assertTrue(chunked.hasCollision());
	}

	private static byte[] readShatteredPdf(String name) throws IOException
	{
		try (var in = Sha1DcMessageDigestTest.class.getResourceAsStream("/sha1dc/" + name))
		{
			return Objects.requireNonNull(in, name).readAllBytes();
		}
	}
}