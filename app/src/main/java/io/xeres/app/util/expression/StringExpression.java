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

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public abstract class StringExpression implements Expression
{
	public enum Operator
	{
		CONTAINS_ANY,
		CONTAINS_ALL,
		EQUALS
	}

	abstract String getValue(FileEntry fileEntry);

	abstract String getType();

	private final Operator operator;
	private final String template;
	private List<String> words;
	private final boolean caseSensitive;

	protected StringExpression(Operator operator, String template, boolean caseSensitive)
	{
		this.operator = operator;
		this.template = caseSensitive ? template : template.toLowerCase(Locale.ENGLISH);
		this.caseSensitive = caseSensitive;
	}

	private List<String> splitWords()
	{
		if (words == null)
		{
			words = Arrays.stream(template.split(" ")).toList();
		}
		return words;
	}

	@Override
	public boolean evaluate(FileEntry fileEntry)
	{
		var value = getValue(fileEntry);
		if (!caseSensitive)
		{
			value = value.toLowerCase(Locale.ENGLISH);
		}

		return switch (operator)
		{
			case EQUALS -> template.equals(value);
			case CONTAINS_ALL -> splitWords().stream().allMatch(value::contains);
			case CONTAINS_ANY -> splitWords().stream().anyMatch(value::contains);
		};
	}

	@Override
	public String toString()
	{
		return switch (operator)
		{
			case CONTAINS_ALL -> getType() + " CONTAINS ALL " + template;
			case CONTAINS_ANY ->
			{
				if (splitWords().size() == 1)
				{
					yield getType() + " CONTAINS " + template;
				}
				else
				{
					yield getType() + " CONTAINS ONE OF " + template;
				}
			}
			case EQUALS ->
			{
				if (splitWords().size() == 1)
				{
					yield getType() + " IS " + template;
				}
				else
				{
					yield getType() + " IS ONE OF " + template;
				}
			}
		};
	}
}
