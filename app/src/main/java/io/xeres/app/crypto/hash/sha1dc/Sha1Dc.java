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

import java.util.Arrays;

import static java.lang.Integer.*;

/// Pure Java implementation of SHA-1 from FIPS 180-1 / RFC 3174 courtesy of [JGit](https://github.com/eclipse-jgit/jgit).
/// [License](https://www.eclipse.org/org/documents/edl-v10.php).
///
/// See [RFC 3174](https://tools.ietf.org/html/rfc3174).
///
/// Unlike MessageDigest, this implementation includes the algorithm used by
/// `sha1dc` to detect cryptanalytic collision attacks against SHA-1, such
/// as the one used by [SHAttered](https://shattered.it/). See
/// [sha1collisiondetection](https://github.com/cr-marcstevens/sha1collisiondetection) for more information.
///
/// When a collision attack is detected, the offending block is compressed three
/// times instead of once (safe hash), so colliding messages end up with different
/// digests. Use [#hasCollision()] to check whether a collision was detected.
///
/// @see <a href="https://github.com/eclipse-jgit/jgit/tree/master/org.eclipse.jgit/src/org/eclipse/jgit/util/sha1">original site</a>
public class Sha1Dc
{
	private final State h = new State();
	private final int[] w = new int[80];

	/// Buffer to accumulate partial blocks to 64 byte alignment.
	private final byte[] buffer = new byte[64];

	/// Total number of bytes in the message.
	private long length;

	private boolean foundCollision;

	private final int[] w2 = new int[80];
	private final State state58 = new State();
	private final State state65 = new State();
	private final State hIn = new State();
	private final State hTmp = new State();

	public Sha1Dc()
	{
		h.init();
	}

	public void update(byte b)
	{
		int bufferLen = (int) (length & 63);
		length++;
		buffer[bufferLen] = b;
		if (bufferLen == 63)
		{
			compress(buffer, 0);
		}
	}

	public void update(byte[] in)
	{
		update(in, 0, in.length);
	}

	public void update(byte[] in, int p, int len)
	{
		// SHA-1 compress can only process whole 64 byte blocks.
		// Hold partial updates in buffer, whose length is the low bits.
		int bufferLen = (int) (length & 63);
		length += len;

		if (bufferLen > 0)
		{
			int n = Math.min(64 - bufferLen, len);
			System.arraycopy(in, p, buffer, bufferLen, n);
			p += n;
			len -= n;
			if (bufferLen + n < 64)
			{
				return;
			}
			compress(buffer, 0);
		}
		while (len >= 64)
		{
			compress(in, p);
			p += 64;
			len -= 64;
		}
		if (len > 0)
		{
			System.arraycopy(in, p, buffer, 0, len);
		}
	}

	private void compress(byte[] block, int p)
	{
		initBlock(block, p);
		int ubcDvMask = UbcCheck.check(w);
		compress();

		while (ubcDvMask != 0)
		{
			int b = numberOfTrailingZeros(lowestOneBit(ubcDvMask));
			UbcCheck.DvInfo dv = UbcCheck.DV[b];
			for (var i = 0; i < 80; i++)
			{
				w2[i] = w[i] ^ dv.dm[i];
			}
			recompress(dv.testt);
			if (eq(hTmp, h))
			{
				foundCollision = true;
				// Safe hash: compress the colliding block two more times (three in total).
				// As colliding messages have different block contents, their digests diverge here.
				compress();
				compress();
				break;
			}
			ubcDvMask &= ~(1 << b);
		}
	}

	private void initBlock(byte[] block, int p)
	{
		for (var t = 0; t < 16; t++)
		{
			w[t] = NB.decodeInt32(block, p + (t << 2));
		}

		// RFC 3174 6.1.b, extend state vector to 80 words.
		for (var t = 16; t < 80; t++)
		{
			int x = w[t - 3] ^ w[t - 8] ^ w[t - 14] ^ w[t - 16];
			w[t] = rotateLeft(x, 1); // S^1(...)
		}
	}

	private void compress()
	{
		// Method 1 from RFC 3174 section 6.1.
		// Method 2 (circular queue of 16 words) is slower.
		int a = h.a, b = h.b, c = h.c, d = h.d, e = h.e;

		// @formatter:off
		e += s1(a, b, c, d, w[0]);
		b = rotateLeft(b, 30);
		d += s1(e, a, b, c, w[1]);
		a = rotateLeft(a, 30);
		c += s1(d, e, a, b, w[2]);
		e = rotateLeft(e, 30);
		b += s1(c, d, e, a, w[3]);
		d = rotateLeft(d, 30);
		a += s1(b, c, d, e, w[4]);
		c = rotateLeft(c, 30);
		e += s1(a, b, c, d, w[5]);
		b = rotateLeft(b, 30);
		d += s1(e, a, b, c, w[6]);
		a = rotateLeft(a, 30);
		c += s1(d, e, a, b, w[7]);
		e = rotateLeft(e, 30);
		b += s1(c, d, e, a, w[8]);
		d = rotateLeft(d, 30);
		a += s1(b, c, d, e, w[9]);
		c = rotateLeft(c, 30);
		e += s1(a, b, c, d, w[10]);
		b = rotateLeft(b, 30);
		d += s1(e, a, b, c, w[11]);
		a = rotateLeft(a, 30);
		c += s1(d, e, a, b, w[12]);
		e = rotateLeft(e, 30);
		b += s1(c, d, e, a, w[13]);
		d = rotateLeft(d, 30);
		a += s1(b, c, d, e, w[14]);
		c = rotateLeft(c, 30);
		e += s1(a, b, c, d, w[15]);
		b = rotateLeft(b, 30);
		d += s1(e, a, b, c, w[16]);
		a = rotateLeft(a, 30);
		c += s1(d, e, a, b, w[17]);
		e = rotateLeft(e, 30);
		b += s1(c, d, e, a, w[18]);
		d = rotateLeft(d, 30);
		a += s1(b, c, d, e, w[19]);
		c = rotateLeft(c, 30);

		e += s2(a, b, c, d, w[20]);
		b = rotateLeft(b, 30);
		d += s2(e, a, b, c, w[21]);
		a = rotateLeft(a, 30);
		c += s2(d, e, a, b, w[22]);
		e = rotateLeft(e, 30);
		b += s2(c, d, e, a, w[23]);
		d = rotateLeft(d, 30);
		a += s2(b, c, d, e, w[24]);
		c = rotateLeft(c, 30);
		e += s2(a, b, c, d, w[25]);
		b = rotateLeft(b, 30);
		d += s2(e, a, b, c, w[26]);
		a = rotateLeft(a, 30);
		c += s2(d, e, a, b, w[27]);
		e = rotateLeft(e, 30);
		b += s2(c, d, e, a, w[28]);
		d = rotateLeft(d, 30);
		a += s2(b, c, d, e, w[29]);
		c = rotateLeft(c, 30);
		e += s2(a, b, c, d, w[30]);
		b = rotateLeft(b, 30);
		d += s2(e, a, b, c, w[31]);
		a = rotateLeft(a, 30);
		c += s2(d, e, a, b, w[32]);
		e = rotateLeft(e, 30);
		b += s2(c, d, e, a, w[33]);
		d = rotateLeft(d, 30);
		a += s2(b, c, d, e, w[34]);
		c = rotateLeft(c, 30);
		e += s2(a, b, c, d, w[35]);
		b = rotateLeft(b, 30);
		d += s2(e, a, b, c, w[36]);
		a = rotateLeft(a, 30);
		c += s2(d, e, a, b, w[37]);
		e = rotateLeft(e, 30);
		b += s2(c, d, e, a, w[38]);
		d = rotateLeft(d, 30);
		a += s2(b, c, d, e, w[39]);
		c = rotateLeft(c, 30);

		e += s3(a, b, c, d, w[40]);
		b = rotateLeft(b, 30);
		d += s3(e, a, b, c, w[41]);
		a = rotateLeft(a, 30);
		c += s3(d, e, a, b, w[42]);
		e = rotateLeft(e, 30);
		b += s3(c, d, e, a, w[43]);
		d = rotateLeft(d, 30);
		a += s3(b, c, d, e, w[44]);
		c = rotateLeft(c, 30);
		e += s3(a, b, c, d, w[45]);
		b = rotateLeft(b, 30);
		d += s3(e, a, b, c, w[46]);
		a = rotateLeft(a, 30);
		c += s3(d, e, a, b, w[47]);
		e = rotateLeft(e, 30);
		b += s3(c, d, e, a, w[48]);
		d = rotateLeft(d, 30);
		a += s3(b, c, d, e, w[49]);
		c = rotateLeft(c, 30);
		e += s3(a, b, c, d, w[50]);
		b = rotateLeft(b, 30);
		d += s3(e, a, b, c, w[51]);
		a = rotateLeft(a, 30);
		c += s3(d, e, a, b, w[52]);
		e = rotateLeft(e, 30);
		b += s3(c, d, e, a, w[53]);
		d = rotateLeft(d, 30);
		a += s3(b, c, d, e, w[54]);
		c = rotateLeft(c, 30);
		e += s3(a, b, c, d, w[55]);
		b = rotateLeft(b, 30);
		d += s3(e, a, b, c, w[56]);
		a = rotateLeft(a, 30);
		c += s3(d, e, a, b, w[57]);
		e = rotateLeft(e, 30);
		state58.save(a, b, c, d, e);
		b += s3(c, d, e, a, w[58]);
		d = rotateLeft(d, 30);
		a += s3(b, c, d, e, w[59]);
		c = rotateLeft(c, 30);

		e += s4(a, b, c, d, w[60]);
		b = rotateLeft(b, 30);
		d += s4(e, a, b, c, w[61]);
		a = rotateLeft(a, 30);
		c += s4(d, e, a, b, w[62]);
		e = rotateLeft(e, 30);
		b += s4(c, d, e, a, w[63]);
		d = rotateLeft(d, 30);
		a += s4(b, c, d, e, w[64]);
		c = rotateLeft(c, 30);
		state65.save(a, b, c, d, e);
		e += s4(a, b, c, d, w[65]);
		b = rotateLeft(b, 30);
		d += s4(e, a, b, c, w[66]);
		a = rotateLeft(a, 30);
		c += s4(d, e, a, b, w[67]);
		e = rotateLeft(e, 30);
		b += s4(c, d, e, a, w[68]);
		d = rotateLeft(d, 30);
		a += s4(b, c, d, e, w[69]);
		c = rotateLeft(c, 30);
		e += s4(a, b, c, d, w[70]);
		b = rotateLeft(b, 30);
		d += s4(e, a, b, c, w[71]);
		a = rotateLeft(a, 30);
		c += s4(d, e, a, b, w[72]);
		e = rotateLeft(e, 30);
		b += s4(c, d, e, a, w[73]);
		d = rotateLeft(d, 30);
		a += s4(b, c, d, e, w[74]);
		c = rotateLeft(c, 30);
		e += s4(a, b, c, d, w[75]);
		b = rotateLeft(b, 30);
		d += s4(e, a, b, c, w[76]);
		a = rotateLeft(a, 30);
		c += s4(d, e, a, b, w[77]);
		e = rotateLeft(e, 30);
		b += s4(c, d, e, a, w[78]);
		d = rotateLeft(d, 30);
		a += s4(b, c, d, e, w[79]);
		c = rotateLeft(c, 30);

		// @formatter:on
		h.save(h.a + a, h.b + b, h.c + c, h.d + d, h.e + e);
	}

	private void recompress(int t)
	{
		State s = switch (t)
		{
			case 58 -> state58;
			case 65 -> state65;
			default -> throw new IllegalStateException();
		};
		int a = s.a;
		int b = s.b;
		int c = s.c;
		int d = s.d;
		int e = s.e;

		// @formatter:off
		if (t == 65)
		{
			{
				c = rotateRight(c, 30);
				a -= s4(b, c, d, e, w2[64]);
			}
			{
				d = rotateRight(d, 30);
				b -= s4(c, d, e, a, w2[63]);
			}
			{
				e = rotateRight(e, 30);
				c -= s4(d, e, a, b, w2[62]);
			}
			{
				a = rotateRight(a, 30);
				d -= s4(e, a, b, c, w2[61]);
			}
			{
				b = rotateRight(b, 30);
				e -= s4(a, b, c, d, w2[60]);
			}

			{
				c = rotateRight(c, 30);
				a -= s3(b, c, d, e, w2[59]);
			}
			{
				d = rotateRight(d, 30);
				b -= s3(c, d, e, a, w2[58]);
			}
		}
		{
			e = rotateRight(e, 30);
			c -= s3(d, e, a, b, w2[57]);
		}
		{
			a = rotateRight(a, 30);
			d -= s3(e, a, b, c, w2[56]);
		}
		{
			b = rotateRight(b, 30);
			e -= s3(a, b, c, d, w2[55]);
		}
		{
			c = rotateRight(c, 30);
			a -= s3(b, c, d, e, w2[54]);
		}
		{
			d = rotateRight(d, 30);
			b -= s3(c, d, e, a, w2[53]);
		}
		{
			e = rotateRight(e, 30);
			c -= s3(d, e, a, b, w2[52]);
		}
		{
			a = rotateRight(a, 30);
			d -= s3(e, a, b, c, w2[51]);
		}
		{
			b = rotateRight(b, 30);
			e -= s3(a, b, c, d, w2[50]);
		}
		{
			c = rotateRight(c, 30);
			a -= s3(b, c, d, e, w2[49]);
		}
		{
			d = rotateRight(d, 30);
			b -= s3(c, d, e, a, w2[48]);
		}
		{
			e = rotateRight(e, 30);
			c -= s3(d, e, a, b, w2[47]);
		}
		{
			a = rotateRight(a, 30);
			d -= s3(e, a, b, c, w2[46]);
		}
		{
			b = rotateRight(b, 30);
			e -= s3(a, b, c, d, w2[45]);
		}
		{
			c = rotateRight(c, 30);
			a -= s3(b, c, d, e, w2[44]);
		}
		{
			d = rotateRight(d, 30);
			b -= s3(c, d, e, a, w2[43]);
		}
		{
			e = rotateRight(e, 30);
			c -= s3(d, e, a, b, w2[42]);
		}
		{
			a = rotateRight(a, 30);
			d -= s3(e, a, b, c, w2[41]);
		}
		{
			b = rotateRight(b, 30);
			e -= s3(a, b, c, d, w2[40]);
		}

		{
			c = rotateRight(c, 30);
			a -= s2(b, c, d, e, w2[39]);
		}
		{
			d = rotateRight(d, 30);
			b -= s2(c, d, e, a, w2[38]);
		}
		{
			e = rotateRight(e, 30);
			c -= s2(d, e, a, b, w2[37]);
		}
		{
			a = rotateRight(a, 30);
			d -= s2(e, a, b, c, w2[36]);
		}
		{
			b = rotateRight(b, 30);
			e -= s2(a, b, c, d, w2[35]);
		}
		{
			c = rotateRight(c, 30);
			a -= s2(b, c, d, e, w2[34]);
		}
		{
			d = rotateRight(d, 30);
			b -= s2(c, d, e, a, w2[33]);
		}
		{
			e = rotateRight(e, 30);
			c -= s2(d, e, a, b, w2[32]);
		}
		{
			a = rotateRight(a, 30);
			d -= s2(e, a, b, c, w2[31]);
		}
		{
			b = rotateRight(b, 30);
			e -= s2(a, b, c, d, w2[30]);
		}
		{
			c = rotateRight(c, 30);
			a -= s2(b, c, d, e, w2[29]);
		}
		{
			d = rotateRight(d, 30);
			b -= s2(c, d, e, a, w2[28]);
		}
		{
			e = rotateRight(e, 30);
			c -= s2(d, e, a, b, w2[27]);
		}
		{
			a = rotateRight(a, 30);
			d -= s2(e, a, b, c, w2[26]);
		}
		{
			b = rotateRight(b, 30);
			e -= s2(a, b, c, d, w2[25]);
		}
		{
			c = rotateRight(c, 30);
			a -= s2(b, c, d, e, w2[24]);
		}
		{
			d = rotateRight(d, 30);
			b -= s2(c, d, e, a, w2[23]);
		}
		{
			e = rotateRight(e, 30);
			c -= s2(d, e, a, b, w2[22]);
		}
		{
			a = rotateRight(a, 30);
			d -= s2(e, a, b, c, w2[21]);
		}
		{
			b = rotateRight(b, 30);
			e -= s2(a, b, c, d, w2[20]);
		}

		{
			c = rotateRight(c, 30);
			a -= s1(b, c, d, e, w2[19]);
		}
		{
			d = rotateRight(d, 30);
			b -= s1(c, d, e, a, w2[18]);
		}
		{
			e = rotateRight(e, 30);
			c -= s1(d, e, a, b, w2[17]);
		}
		{
			a = rotateRight(a, 30);
			d -= s1(e, a, b, c, w2[16]);
		}
		{
			b = rotateRight(b, 30);
			e -= s1(a, b, c, d, w2[15]);
		}
		{
			c = rotateRight(c, 30);
			a -= s1(b, c, d, e, w2[14]);
		}
		{
			d = rotateRight(d, 30);
			b -= s1(c, d, e, a, w2[13]);
		}
		{
			e = rotateRight(e, 30);
			c -= s1(d, e, a, b, w2[12]);
		}
		{
			a = rotateRight(a, 30);
			d -= s1(e, a, b, c, w2[11]);
		}
		{
			b = rotateRight(b, 30);
			e -= s1(a, b, c, d, w2[10]);
		}
		{
			c = rotateRight(c, 30);
			a -= s1(b, c, d, e, w2[9]);
		}
		{
			d = rotateRight(d, 30);
			b -= s1(c, d, e, a, w2[8]);
		}
		{
			e = rotateRight(e, 30);
			c -= s1(d, e, a, b, w2[7]);
		}
		{
			a = rotateRight(a, 30);
			d -= s1(e, a, b, c, w2[6]);
		}
		{
			b = rotateRight(b, 30);
			e -= s1(a, b, c, d, w2[5]);
		}
		{
			c = rotateRight(c, 30);
			a -= s1(b, c, d, e, w2[4]);
		}
		{
			d = rotateRight(d, 30);
			b -= s1(c, d, e, a, w2[3]);
		}
		{
			e = rotateRight(e, 30);
			c -= s1(d, e, a, b, w2[2]);
		}
		{
			a = rotateRight(a, 30);
			d -= s1(e, a, b, c, w2[1]);
		}
		{
			b = rotateRight(b, 30);
			e -= s1(a, b, c, d, w2[0]);
		}

		hIn.save(a, b, c, d, e);
		a = s.a;
		b = s.b;
		c = s.c;
		d = s.d;
		e = s.e;

		if (t == 58)
		{
			{
				b += s3(c, d, e, a, w2[58]);
				d = rotateLeft(d, 30);
			}
			{
				a += s3(b, c, d, e, w2[59]);
				c = rotateLeft(c, 30);
			}

			{
				e += s4(a, b, c, d, w2[60]);
				b = rotateLeft(b, 30);
			}
			{
				d += s4(e, a, b, c, w2[61]);
				a = rotateLeft(a, 30);
			}
			{
				c += s4(d, e, a, b, w2[62]);
				e = rotateLeft(e, 30);
			}
			{
				b += s4(c, d, e, a, w2[63]);
				d = rotateLeft(d, 30);
			}
			{
				a += s4(b, c, d, e, w2[64]);
				c = rotateLeft(c, 30);
			}
		}
		{
			e += s4(a, b, c, d, w2[65]);
			b = rotateLeft(b, 30);
		}
		{
			d += s4(e, a, b, c, w2[66]);
			a = rotateLeft(a, 30);
		}
		{
			c += s4(d, e, a, b, w2[67]);
			e = rotateLeft(e, 30);
		}
		{
			b += s4(c, d, e, a, w2[68]);
			d = rotateLeft(d, 30);
		}
		{
			a += s4(b, c, d, e, w2[69]);
			c = rotateLeft(c, 30);
		}
		{
			e += s4(a, b, c, d, w2[70]);
			b = rotateLeft(b, 30);
		}
		{
			d += s4(e, a, b, c, w2[71]);
			a = rotateLeft(a, 30);
		}
		{
			c += s4(d, e, a, b, w2[72]);
			e = rotateLeft(e, 30);
		}
		{
			b += s4(c, d, e, a, w2[73]);
			d = rotateLeft(d, 30);
		}
		{
			a += s4(b, c, d, e, w2[74]);
			c = rotateLeft(c, 30);
		}
		{
			e += s4(a, b, c, d, w2[75]);
			b = rotateLeft(b, 30);
		}
		{
			d += s4(e, a, b, c, w2[76]);
			a = rotateLeft(a, 30);
		}
		{
			c += s4(d, e, a, b, w2[77]);
			e = rotateLeft(e, 30);
		}
		{
			b += s4(c, d, e, a, w2[78]);
			d = rotateLeft(d, 30);
		}
		{
			a += s4(b, c, d, e, w2[79]);
			c = rotateLeft(c, 30);
		}

		// @formatter:on
		hTmp.save(hIn.a + a, hIn.b + b, hIn.c + c, hIn.d + d, hIn.e + e);
	}

	private static int s1(int a, int b, int c, int d, int wT)
	{
		return rotateLeft(a, 5)
				// f: 0 <= t <= 19
				+ ((b & c) | (~b & d))
				+ 0x5A827999 + wT;
	}

	private static int s2(int a, int b, int c, int d, int wT)
	{
		return rotateLeft(a, 5)
				// f: 20 <= t <= 39
				+ (b ^ c ^ d)
				+ 0x6ED9EBA1 + wT;
	}

	private static int s3(int a, int b, int c, int d, int wT)
	{
		return rotateLeft(a, 5)
				// f: 40 <= t <= 59
				+ ((b & c) | (b & d) | (c & d))
				+ 0x8F1BBCDC + wT;
	}

	private static int s4(int a, int b, int c, int d, int wT)
	{
		return rotateLeft(a, 5)
				// f: 60 <= t <= 79
				+ (b ^ c ^ d)
				+ 0xCA62C1D6 + wT;
	}

	private static boolean eq(State q, State r)
	{
		return q.a == r.a
				&& q.b == r.b
				&& q.c == r.c
				&& q.d == r.d
				&& q.e == r.e;
	}

	private void finish()
	{
		int bufferLen = (int) (length & 63);
		if (bufferLen > 55)
		{
			// Last block is too small; pad, compress, pad another block.
			buffer[bufferLen++] = (byte) 0x80;
			Arrays.fill(buffer, bufferLen, 64, (byte) 0);
			compress(buffer, 0);
			Arrays.fill(buffer, 0, 56, (byte) 0);
		}
		else
		{
			// Last block can hold padding and length.
			buffer[bufferLen++] = (byte) 0x80;
			Arrays.fill(buffer, bufferLen, 56, (byte) 0);
		}

		// SHA-1 appends the length of the message in bits after the
		// padding block (above). Here length is in bytes. Multiply by
		// 8 by shifting by 3 as part of storing the 64 bit byte length
		// into the two words expected in the trailer.
		NB.encodeInt32(buffer, 56, (int) (length >>> (32 - 3)));
		NB.encodeInt32(buffer, 60, (int) (length << 3));
		compress(buffer, 0);
	}

	public byte[] digest()
	{
		finish();

		var b = new byte[20];
		NB.encodeInt32(b, 0, h.a);
		NB.encodeInt32(b, 4, h.b);
		NB.encodeInt32(b, 8, h.c);
		NB.encodeInt32(b, 12, h.d);
		NB.encodeInt32(b, 16, h.e);
		return b;
	}

	public boolean hasCollision()
	{
		return foundCollision;
	}

	public Sha1Dc reset()
	{
		h.init();
		length = 0;
		foundCollision = false;
		return this;
	}

	private static final class State
	{
		int a;
		int b;
		int c;
		int d;
		int e;

		void init()
		{
			// Magic initialization constants defined by FIPS180.
			save(0x67452301, 0xEFCDAB89, 0x98BADCFE, 0x10325476, 0xC3D2E1F0);
		}

		void save(int a1, int b1, int c1, int d1, int e1)
		{
			a = a1;
			b = b1;
			c = c1;
			d = d1;
			e = e1;
		}
	}
}
