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
 * Matches the popularity of a file. Always returns no match because local files
 * don't have any metadata indicating the popularity.
 * <p>
 * RS does the same.
 */
public class PopularityExpression extends RelationalExpression
{
	public PopularityExpression(Operator operator, Integer lowerValue, Integer higherValue)
	{
		super(operator, lowerValue, higherValue);
	}

	@Override
	boolean isEnabled()
	{
		return false;
	}

	@Override
	String getType()
	{
		return "POPULARITY";
	}

	@Override
	String getDatabaseColumnName()
	{
		return "";
	}

	@Override
	int getValue(File file)
	{
		return 0; // Popularity is not used
	}
}
