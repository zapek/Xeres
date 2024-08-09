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
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * Matches the size of the file. Only works for files bigger than 2 GB. Since it also uses a 32-bit integer, the precision
 * is limited and there's some trickery to make it work but do not expect it to be very precise.
 * <p>
 * The maximum file size is 2.147 TB.
 */
public class SizeMbExpression extends RelationalExpression
{
	public SizeMbExpression(Operator operator, int lowerValue, int higherValue)
	{
		super(operator, lowerValue, higherValue);
	}

	@Override
	String getType()
	{
		return "SIZE";
	}

	@Override
	String getFieldName()
	{
		return "size";
	}

	@Override
	public Predicate toPredicate(CriteriaBuilder cb, Root<File> root)
	{
		long lower;
		long higher;

		// We need to restore the value with the most pessimistic loss so that the comparison makes sense
		switch (operator)
		{
			case EQUALS ->
			{
				lower = getPessimisticValue(lowerValue);
				higher = getOptimisticValue(lowerValue);
			}
			case GREATER_THAN_OR_EQUALS, GREATER_THAN ->
			{
				lower = getOptimisticValue(lowerValue);
				higher = getOptimisticValue(lowerValue);
			}
			case LESSER_THAN_OR_EQUALS, LESSER_THAN ->
			{
				lower = getPessimisticValue(lowerValue);
				higher = getPessimisticValue(lowerValue);
			}
			case IN_RANGE ->
			{
				lower = getPessimisticValue(lowerValue);
				higher = getOptimisticValue(higherValue);
			}
			default -> throw new IllegalStateException("Unexpected operator: " + operator);
		}

		// Remember: it's the condition that is checked to be true, i.e. greater than means the expression value is greater than the value of the file
		return switch (operator)
		{
			case EQUALS, IN_RANGE -> cb.between(root.get(getFieldName()), lower, higher);
			case GREATER_THAN_OR_EQUALS -> cb.lessThanOrEqualTo(root.get(getFieldName()), lower);
			case GREATER_THAN -> cb.lessThan(root.get(getFieldName()), lower);
			case LESSER_THAN_OR_EQUALS -> cb.greaterThanOrEqualTo(root.get(getFieldName()), lower);
			case LESSER_THAN -> cb.greaterThan(root.get(getFieldName()), lower);
		};
	}

	private static long getPessimisticValue(int value)
	{
		return (long) value << 20;
	}

	private static long getOptimisticValue(int value)
	{
		return (long) value << 20 | 0xfffff;
	}

	@Override
	int getValue(File file)
	{
		return (int) (file.getSize() >> 20); // the max value that this check can handle is (2 ^ 31 - 1) * 2 ^ 20, which is 2.147 TB
	}
}
