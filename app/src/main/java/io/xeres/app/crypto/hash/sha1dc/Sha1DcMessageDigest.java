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

import io.xeres.app.crypto.hash.AbstractMessageDigest;
import io.xeres.common.id.Sha1Sum;

import java.nio.ByteBuffer;

/// SHA1 using the collision digest detection. Use it for payloads
/// bigger than 64 bytes (one block is 64 bytes, attack needs at least 2) and
/// where increased CPU usage (up to 8x more comparing to JDK's SHA1)
/// is acceptable.
public class Sha1DcMessageDigest extends AbstractMessageDigest
{
	private final Sha1Dc sha1Dc;
	private byte[] result;
	private boolean hasCollision;

	public Sha1DcMessageDigest()
	{
		super("SHA-1");
		sha1Dc = new Sha1Dc();
	}

	public Sha1Sum getSum()
	{
		return new Sha1Sum(getBytes());
	}

	/// Checks if the hash has a collision. Can only be called
	/// after [#getBytes()].
	public boolean hasCollision()
	{
		return hasCollision;
	}

	@Override
	public void update(byte[] input)
	{
		resetCompletion();
		sha1Dc.update(input);
	}

	@Override
	public void update(byte[] input, int offset, int length)
	{
		resetCompletion();
		sha1Dc.update(input, offset, length);
	}

	@Override
	public void update(ByteBuffer input)
	{
		resetCompletion();
		if (input.hasArray())
		{
			sha1Dc.update(input.array(), input.arrayOffset() + input.position(), input.remaining());
			input.position(input.limit());
		}
		else
		{
			var buf = new byte[Math.min(input.remaining(), 8192)];
			while (input.hasRemaining())
			{
				var n = Math.min(input.remaining(), buf.length);
				input.get(buf, 0, n);
				sha1Dc.update(buf, 0, n);
			}
		}
	}

	@Override
	public byte[] getBytes()
	{
		completeIfNeeded();
		return result.clone();
	}

	private void completeIfNeeded()
	{
		if (result == null)
		{
			result = sha1Dc.digest();
			hasCollision = sha1Dc.hasCollision();
			sha1Dc.reset(); // Ready for update() to be called again.
		}
	}

	private void resetCompletion()
	{
		result = null;
		hasCollision = false;
	}
}
