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

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.EnumSet;
import java.util.Set;

@Converter
public abstract class EnumSetConverter<E extends Enum<E>> implements AttributeConverter<Set<E>, Integer>
{
	abstract Class<E> getEnumClass();

	@Override
	public Integer convertToDatabaseColumn(Set<E> enumSet)
	{
		var value = 0;

		if (enumSet != null)
		{
			for (Enum<?> anEnum : enumSet)
			{
				value |= 1 << anEnum.ordinal();
			}
		}
		return value;
	}

	@Override
	public Set<E> convertToEntityAttribute(Integer value)
	{
		var e = getEnumClass();

		var enumSet = EnumSet.noneOf(e);
		for (E enumConstant : e.getEnumConstants())
		{
			if ((value & (1 << enumConstant.ordinal())) != 0)
			{
				enumSet.add(enumConstant);
			}
		}
		return enumSet;
	}
}
