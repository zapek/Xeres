/*
 * Copyright (c) 2019-2020 by David Gerber - https://zapek.com
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

package io.xeres.common.id;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Arrays;

/**
 * Interface that represents an identifier of an object in the Retroshare protocol that doesn't fit
 * in a primitive type.
 */
public interface Identifier
{
	/**
	 * Gets a byte representation of the identifier.
	 *
	 * @return an array of bytes containing the identifier
	 */
	byte[] getBytes();

	/**
	 * Gets how many bytes are needed to store the identifier.
	 *
	 * @return the length of the identifier
	 */
	int getLength();

	/**
	 * Gets the representation of the identifier. To be used every time the identity is needed
	 * as a string (UI, headers, etc...).
	 *
	 * @return a string representation
	 */
	String toString();

	@JsonIgnore
	default byte[] getNullIdentifier()
	{
		var identifier = new byte[getLength()];
		Arrays.fill(identifier, (byte) 0);
		return identifier;
	}
}
