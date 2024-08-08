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

package io.xeres.app.util.expression;

import io.xeres.app.database.model.file.File;

public class HashExpression extends StringExpression
{
	public HashExpression(Operator operator, String template)
	{
		super(operator, template, false);
	}

	@Override
	String getType()
	{
		return "HASH";
	}

	@Override
	String getFieldName()
	{
		return "hash"; // XXX: prolly won't work...
	}

	@Override
	String getValue(File file)
	{
		return file.getHash().toString();
	}
}
