/*
 * Copyright (c) 2019-2021 by David Gerber - https://zapek.com
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

package io.xeres.app.database.converter;

import io.xeres.app.crypto.rsa.RSA;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;

import static org.apache.commons.lang3.ArrayUtils.isEmpty;

@Converter
public class PrivateKeyConverter implements AttributeConverter<PrivateKey, byte[]>
{
	@Override
	public byte[] convertToDatabaseColumn(PrivateKey attribute)
	{
		return attribute != null ? attribute.getEncoded() : null;
	}

	@Override
	public PrivateKey convertToEntityAttribute(byte[] dbData)
	{
		if (isEmpty(dbData))
		{
			return null;
		}

		try
		{
			return RSA.getPrivateKey(dbData);
		}
		catch (NoSuchAlgorithmException | InvalidKeySpecException e)
		{
			throw new IllegalArgumentException("Cannot read PrivateKey from database: " + e.getMessage(), e);
		}
	}
}
