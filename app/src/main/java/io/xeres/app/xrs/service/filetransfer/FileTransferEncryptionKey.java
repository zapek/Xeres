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

package io.xeres.app.xrs.service.filetransfer;

import io.xeres.app.crypto.hash.sha256.Sha256MessageDigest;
import io.xeres.common.id.Sha1Sum;

import javax.crypto.SecretKey;
import java.io.Serial;

class FileTransferEncryptionKey implements SecretKey
{
	@Serial
	private static final long serialVersionUID = 6540345707970134182L;

	private final byte[] encoded;

	public FileTransferEncryptionKey(Sha1Sum hash)
	{
		var digest = new Sha256MessageDigest();
		digest.update(hash.getBytes());
		encoded = digest.getBytes();
	}

	@Override
	public String getAlgorithm()
	{
		return "ChaCha20";
	}

	@Override
	public String getFormat()
	{
		return "RAW";
	}

	@Override
	public byte[] getEncoded()
	{
		return encoded.clone();
	}
}
