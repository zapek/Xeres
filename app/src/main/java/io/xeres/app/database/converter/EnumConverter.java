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

package io.xeres.app.database.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * This class is needed because Hibernate uses the ordinal value of enums to save them in the database and
 * some smartass changed enums in H2 to start from 1 instead of 0. Of course this breaks everything.
 */
@Converter
public abstract class EnumConverter<E extends Enum<E>> implements AttributeConverter<E, Integer>
{
	abstract Class<E> getEnumClass();

	@Override
	public Integer convertToDatabaseColumn(E attribute)
	{
		if (attribute == null)
		{
			return null;
		}
		return attribute.ordinal() + 1;
	}

	@Override
	public E convertToEntityAttribute(Integer value)
	{
		if (value == null)
		{
			return null;
		}

		var e = getEnumClass();

		for (var enumConstant : e.getEnumConstants())
		{
			if (value == enumConstant.ordinal() + 1)
			{
				return enumConstant;
			}
		}
		throw new IllegalArgumentException("Ordinal value " + value + " doesn't exist for enum " + e);
	}
}
