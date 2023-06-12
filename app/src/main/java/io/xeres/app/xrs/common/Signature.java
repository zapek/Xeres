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

import io.xeres.app.database.converter.SignatureTypeConverter;
import io.xeres.common.id.GxsId;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.util.Arrays;
import java.util.Objects;

@Embeddable
public final class Signature implements Comparable<Signature>
{
	@Convert(converter = SignatureTypeConverter.class)
	private Type type;

	@Embedded
	@NotNull
	@AttributeOverride(name = "identifier", column = @Column(name = "gxs_id"))
	private GxsId gxsId;

	private byte[] data;

	public Signature()
	{
	}

	public Signature(Type type, @NotNull GxsId gxsId, byte[] data)
	{
		this.type = type;
		this.gxsId = gxsId;
		this.data = data;
	}

	public Signature(@NotNull GxsId gxsId, byte[] data)
	{
		this.gxsId = gxsId;
		this.data = data;
	}

	public Type getType()
	{
		return type;
	}

	public void setType(Type type)
	{
		this.type = type;
	}

	public @NotNull GxsId getGxsId()
	{
		return gxsId;
	}

	public void setGxsId(@NotNull GxsId gxsId)
	{
		this.gxsId = gxsId;
	}

	public byte[] getData()
	{
		return data;
	}

	public void setData(byte[] data)
	{
		this.data = data;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Signature signature = (Signature) o;
		return Objects.equals(gxsId, signature.gxsId);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(gxsId);
	}

	@Override
	public int compareTo(Signature o)
	{
		return type.getValue() - o.type.getValue();
	}

	@Override
	public String toString()
	{
		return "Signature{" +
				"gxsId=" + gxsId +
				'}';
	}

	public enum Type
	{
		AUTHOR(0x10), // RS calls it IDENTITY
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

		public static Signature.Type findByValue(int value)
		{
			return Arrays.stream(values()).filter(type -> type.getValue() == value).findFirst().orElseThrow();
		}
	}
}
