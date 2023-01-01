/*
 * Copyright (c) 2019-2023 by David Gerber - https://zapek.com
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

package io.xeres.app.xrs.common;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class SignatureSet
{
	public enum Type
	{
		IDENTITY(0x10),
		PUBLISH(0x20),
		ADMIN(0x40);

		Type(int value)
		{
			this.value = value;
		}

		private final int value;

		public int getValue()
		{
			return value;
		}

		public static Type findByValue(int value)
		{
			return Arrays.stream(values()).filter(type -> type.getValue() == value).findFirst().orElseThrow();
		}
	}

	private final Map<Integer, Signature> signatures = new HashMap<>();

	public SignatureSet()
	{
		// Needed
	}

	public void put(Type type, Signature signature)
	{
		signatures.put(type.getValue(), signature);
	}

	public Map<Integer, Signature> getSignatures()
	{
		return signatures;
	}
}
