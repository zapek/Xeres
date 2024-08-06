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

public abstract class RelationalExpression<T extends Number> implements Expression
{
	public enum Operator
	{
		EQUALS, // ==
		GREATER_THAN_OR_EQUALS, // >=
		GREATER_THAN, // >
		LESSER_THAN_OR_EQUALS, // <=
		LESSER_THAN, // <
		IN_RANGE
	}

	abstract T getValue(FileEntry fileEntry);

	abstract String getType();

	private final Operator operator;
	private final T lowerValue;
	private final T higherValue;

	protected RelationalExpression(Operator operator, T lowerValue, T higherValue)
	{
		this.operator = operator;
		this.lowerValue = lowerValue;
		this.higherValue = higherValue;
	}

	@Override
	public boolean evaluate(FileEntry fileEntry)
	{
		var value = getValue(fileEntry);

		// Remember: it's the condition that is checked to be true, i.e. greater than means the expression value is greater than the value of the file
		return switch (operator)
		{
			case EQUALS -> lowerValue.longValue() == value.longValue();
			case GREATER_THAN_OR_EQUALS -> lowerValue.longValue() >= value.longValue();
			case GREATER_THAN -> lowerValue.longValue() > value.longValue();
			case LESSER_THAN_OR_EQUALS -> lowerValue.longValue() <= value.longValue();
			case LESSER_THAN -> lowerValue.longValue() < value.longValue();
			case IN_RANGE -> (lowerValue.longValue() <= value.longValue()) && (value.longValue() <= higherValue.longValue());
		};
	}

	@Override
	public String toString()
	{
		return switch (operator)
		{
			case EQUALS -> getType() + " = " + lowerValue.toString();
			case GREATER_THAN_OR_EQUALS -> getType() + " <= " + lowerValue.toString();
			case GREATER_THAN -> getType() + " < " + lowerValue.toString();
			case LESSER_THAN_OR_EQUALS -> getType() + " >= " + lowerValue.toString();
			case LESSER_THAN -> getType() + " > " + lowerValue.toString();
			case IN_RANGE -> lowerValue.toString() + " <= " + getType() + " <= " + higherValue.toString();
		};
	}
}
