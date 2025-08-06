/*
 * Copyright (c) 2024-2025 by David Gerber - https://zapek.com
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

package io.xeres.app.util.expression;

import io.xeres.app.database.model.file.File;

/**
 * Matches the size of the file. Is limited to a maximum file size of a signed 32-bit integer, which is
 * around 2 GB. Use {@link SizeMbExpression} for bigger files.
 */
public class SizeExpression extends RelationalExpression
{
	public SizeExpression(Operator operator, int lowerValue, int higherValue)
	{
		super(operator, lowerValue, higherValue);
	}

	@Override
	String getType()
	{
		return "SIZE";
	}

	@Override
	String getDatabaseColumnName()
	{
		return "size";
	}

	@Override
	int getValue(File file)
	{
		return Math.clamp(file.getSize(), 0, Integer.MAX_VALUE);
	}
}
