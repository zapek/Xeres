/*
 * Copyright (c) 2024 by David Gerber - https://zapek.com
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

package io.xeres.app.crypto.hmac;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public abstract class AbstractHMac
{
	protected final Mac mac;
	private byte[] result;

	protected AbstractHMac(SecretKey secretKey, String algorithm)
	{
		try
		{
			mac = Mac.getInstance(algorithm);
			mac.init(new SecretKeySpec(secretKey.getEncoded(), algorithm));
		}
		catch (NoSuchAlgorithmException | InvalidKeyException e)
		{
			throw new RuntimeException(e);
		}
	}

	public void update(byte[] input)
	{
		resetCompletion();
		mac.update(input);
	}

	public void update(byte[] input, int offset, int length)
	{
		resetCompletion();
		mac.update(input, offset, length);
	}

	public void update(ByteBuffer input)
	{
		resetCompletion();
		mac.update(input);
	}

	public byte[] getBytes()
	{
		completeIfNeeded();
		return result;
	}

	private void completeIfNeeded()
	{
		if (result == null)
		{
			result = mac.doFinal();
		}
	}

	private void resetCompletion()
	{
		result = null;
	}
}
