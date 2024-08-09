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

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Matches the last modified field of the file. This is what is reported by the filesystem
 * and not the metadata of the file, and is hence not very reliable.
 */
public class DateExpression extends RelationalExpression
{
	public DateExpression(Operator operator, int lowerValue, int higherValue)
	{
		super(operator, lowerValue, higherValue);
	}

	@Override
	String getType()
	{
		return "DATE";
	}

	@Override
	String getFieldName()
	{
		return "modified";
	}

	@Override
	public Predicate toPredicate(CriteriaBuilder cb, Root<File> root)
	{
		// Remember: it's the condition that is checked to be true, i.e. greater than means the expression value is greater than the value of the file
		return switch (operator)
		{
			case EQUALS -> cb.equal(root.get(getFieldName()), Instant.ofEpochSecond(lowerValue));
			case GREATER_THAN_OR_EQUALS -> cb.lessThanOrEqualTo(root.get(getFieldName()), Instant.ofEpochSecond(lowerValue));
			case GREATER_THAN -> cb.lessThan(root.get(getFieldName()), Instant.ofEpochSecond(lowerValue));
			case LESSER_THAN_OR_EQUALS -> cb.greaterThanOrEqualTo(root.get(getFieldName()), Instant.ofEpochSecond(lowerValue));
			case LESSER_THAN -> cb.greaterThan(root.get(getFieldName()), Instant.ofEpochSecond(lowerValue));
			case IN_RANGE -> cb.between(root.get(getFieldName()), Instant.ofEpochSecond(lowerValue), Instant.ofEpochSecond(higherValue));
		};
	}

	@Override
	int getValue(File file)
	{
		return (int) file.getModified().truncatedTo(ChronoUnit.SECONDS).getEpochSecond();
	}
}
