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

/**
 * Matches 2 expressions, ANDed, ORed or XORed together.
 */
public class CompoundExpression implements Expression
{
	public enum Operator
	{
		AND,
		OR,
		XOR
	}

	private final Operator operator;
	private final Expression left;
	private final Expression right;

	public CompoundExpression(Operator operator, Expression left, Expression right)
	{
		this.operator = operator;
		this.left = left;
		this.right = right;
	}

	@Override
	public boolean evaluate(File file)
	{
		if (left == null || right == null)
		{
			return false;
		}

		return switch (operator)
		{
			case AND -> left.evaluate(file) && right.evaluate(file);
			case OR -> left.evaluate(file) || right.evaluate(file);
			case XOR -> left.evaluate(file) ^ right.evaluate(file);
		};
	}

	@Override
	public Predicate toPredicate(CriteriaBuilder cb, Root<File> root)
	{
		return switch (operator)
		{
			case AND -> cb.and(left.toPredicate(cb, root), right.toPredicate(cb, root));
			case OR -> cb.or(left.toPredicate(cb, root), right.toPredicate(cb, root));
			case XOR ->
			{
				var l = left.toPredicate(cb, root);
				var r = right.toPredicate(cb, root);
				yield cb.or(cb.and(l, cb.not(r)), cb.and(cb.not(l), r));
			}
		};
	}

	@Override
	public void linearize(List<Byte> tokens, List<Integer> ints, List<String> strings)
	{
		tokens.add(ExpressionType.getTokenValueByClass(getClass()));
		ints.add(operator.ordinal());
		left.linearize(tokens, ints, strings);
		right.linearize(tokens, ints, strings);
	}

	@Override
	public String toString()
	{
		return switch (operator)
		{
			case AND -> "(" + left + ") AND (" + right + ")";
			case OR -> "(" + left + ") OR (" + right + ")";
			case XOR -> "(" + left + ") XOR (" + right + ")";
		};
	}
}
