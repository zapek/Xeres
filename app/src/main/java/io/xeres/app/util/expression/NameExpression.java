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
 * Matches the name of the file.
 */
public class NameExpression extends StringExpression
{
	public NameExpression(Operator operator, String template, boolean caseSensitive)
	{
		super(operator, template, caseSensitive);
	}

	@Override
	String getType()
	{
		return "NAME";
	}

	@Override
	String getDatabaseColumnName()
	{
		return "name";
	}

	@Override
	String getValue(File file)
	{
		return file.getName();
	}
}
