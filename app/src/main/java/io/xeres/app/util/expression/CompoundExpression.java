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

import java.util.List;

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
	public boolean evaluate(FileEntry fileEntry)
	{
		if (left == null || right == null)
		{
			return false;
		}

		return switch (operator)
		{
			case AND -> left.evaluate(fileEntry) && right.evaluate(fileEntry);
			case OR -> left.evaluate(fileEntry) || right.evaluate(fileEntry);
			case XOR -> left.evaluate(fileEntry) ^ right.evaluate(fileEntry);
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
			case AND -> "(" + left.toString() + ") AND (" + right.toString() + ")";
			case OR -> "(" + left.toString() + ") OR (" + right.toString() + ")";
			case XOR -> "(" + left.toString() + ") XOR (" + right.toString() + ")";
		};
	}
}
