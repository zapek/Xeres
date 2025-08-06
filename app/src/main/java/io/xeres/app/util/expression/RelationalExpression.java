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
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.List;

abstract class RelationalExpression implements Expression
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

	boolean isEnabled()
	{
		return true;
	}

	abstract int getValue(File file);

	abstract String getType();

	/**
	 * Gets the column name from the 'FILE' table in the database.
	 *
	 * @return the column name in lowercase. Null if the relation must be ignored.
	 */
	abstract String getDatabaseColumnName();

	protected final Operator operator;
	protected final int lowerValue;
	protected final int higherValue;

	protected RelationalExpression(Operator operator, int lowerValue, int higherValue)
	{
		this.operator = operator;
		this.lowerValue = lowerValue;
		this.higherValue = higherValue;
	}

	@Override
	public boolean evaluate(File file)
	{
		var value = getValue(file);

		// Remember: it's the condition that is checked to be true, i.e. greater than means the expression value is greater than the value of the file
		return switch (operator)
		{
			case EQUALS -> lowerValue == value;
			case GREATER_THAN_OR_EQUALS -> lowerValue >= value;
			case GREATER_THAN -> lowerValue > value;
			case LESSER_THAN_OR_EQUALS -> lowerValue <= value;
			case LESSER_THAN -> lowerValue < value;
			case IN_RANGE -> (lowerValue <= value) && (value <= higherValue);
		};
	}

	@Override
	public Predicate toPredicate(CriteriaBuilder cb, Root<File> root)
	{
		if (!isEnabled())
		{
			return cb.isFalse(cb.literal(true));
		}

		// Remember: it's the condition that is checked to be true, i.e. greater than means the expression value is greater than the value of the file
		return switch (operator)
		{
			case EQUALS -> cb.equal(root.get(getDatabaseColumnName()), lowerValue);
			case GREATER_THAN_OR_EQUALS -> cb.lessThanOrEqualTo(root.get(getDatabaseColumnName()), lowerValue);
			case GREATER_THAN -> cb.lessThan(root.get(getDatabaseColumnName()), lowerValue);
			case LESSER_THAN_OR_EQUALS -> cb.greaterThanOrEqualTo(root.get(getDatabaseColumnName()), lowerValue);
			case LESSER_THAN -> cb.greaterThan(root.get(getDatabaseColumnName()), lowerValue);
			case IN_RANGE -> cb.between(root.get(getDatabaseColumnName()), lowerValue, higherValue);
		};
	}

	@Override
	public void linearize(List<Byte> tokens, List<Integer> ints, List<String> strings)
	{
		tokens.add(ExpressionType.getTokenValueByClass(getClass()));
		ints.add(operator.ordinal());
		ints.add(lowerValue);
		ints.add(higherValue);
	}

	@Override
	public String toString()
	{
		return switch (operator)
		{
			case EQUALS -> getType() + " = " + lowerValue;
			case GREATER_THAN_OR_EQUALS -> getType() + " <= " + lowerValue;
			case GREATER_THAN -> getType() + " < " + lowerValue;
			case LESSER_THAN_OR_EQUALS -> getType() + " >= " + lowerValue;
			case LESSER_THAN -> getType() + " > " + lowerValue;
			case IN_RANGE -> lowerValue + " <= " + getType() + " <= " + higherValue;
		};
	}
}
