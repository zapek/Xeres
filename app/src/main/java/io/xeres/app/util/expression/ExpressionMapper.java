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

import io.xeres.app.xrs.service.turtle.item.TurtleRegExpSearchRequestItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;

public final class ExpressionMapper
{
	private static final Logger log = LoggerFactory.getLogger(ExpressionMapper.class);

	private ExpressionMapper()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	private static class Context
	{
		private final List<Byte> tokens;
		private final List<Integer> ints;
		private final List<String> strings;
		private int tokenIndex;
		private int integerIndex;
		private int stringIndex;

		public Context(List<Byte> tokens, List<Integer> ints, List<String> strings)
		{
			this.tokens = tokens;
			this.ints = ints;
			this.strings = strings;
		}

		public boolean hasNextToken()
		{
			return tokenIndex < tokens.size();
		}

		public ExpressionType nextToken()
		{
			return ExpressionType.values()[tokens.get(tokenIndex++)];
		}

		public int nextIntegerValue()
		{
			return ints.get(integerIndex++);
		}

		public void skipIntegerValue()
		{
			integerIndex++;
		}

		public String nextStringValue()
		{
			return strings.get(stringIndex++);
		}
	}

	public static List<Expression> toExpressions(TurtleRegExpSearchRequestItem item)
	{
		var context = new Context(item.getTokens(), item.getInts(), item.getStrings());
		List<Expression> expressions = new ArrayList<>();

		try
		{
			while (context.hasNextToken())
			{
				expressions.add(toExpression(context));
			}
		}
		catch (IndexOutOfBoundsException | IllegalStateException e)
		{
			log.error("Expression error: {} for the following token input: tokens {}, ints {}, strings {}",
					e.getMessage(),
					Arrays.toString(item.getTokens().toArray()),
					Arrays.toString(item.getInts().toArray()),
					Arrays.toString(item.getStrings().toArray()));
			return List.of();
		}
		return expressions;
	}

	public static TurtleRegExpSearchRequestItem toItem(List<Expression> expressions)
	{
		List<Byte> tokens = new ArrayList<>();
		List<Integer> ints = new ArrayList<>();
		List<String> strings = new ArrayList<>();

		for (var expression : expressions)
		{
			expression.linearize(tokens, ints, strings);
		}
		return new TurtleRegExpSearchRequestItem(tokens, ints, strings);
	}

	private static Expression toExpression(Context context)
	{
		var token = context.nextToken();
		return switch (token)
		{
			case DATE -> toDateExpression(context);
			case POPULARITY -> toPopularityExpression(context);
			case SIZE -> toSizeExpression(context);
			case SIZE_MB -> toSizeMbExpression(context);
			case NAME -> toNameExpression(context);
			case PATH -> toPathExpression(context);
			case EXTENSION -> toExtensionExpression(context);
			case HASH -> toHashExpression(context);
			case COMPOUND -> toCompoundExpression(context);
		};
	}

	private static DateExpression toDateExpression(Context context)
	{
		var operator = RelationalExpression.Operator.values()[context.nextIntegerValue()];
		var lowerValue = context.nextIntegerValue();
		var higherValue = context.nextIntegerValue();
		return new DateExpression(operator, lowerValue, higherValue);
	}

	private static PopularityExpression toPopularityExpression(Context context)
	{
		var operator = RelationalExpression.Operator.values()[context.nextIntegerValue()];
		var lowerValue = context.nextIntegerValue();
		var higherValue = context.nextIntegerValue();
		return new PopularityExpression(operator, lowerValue, higherValue);
	}

	private static SizeExpression toSizeExpression(Context context)
	{
		var operator = RelationalExpression.Operator.values()[context.nextIntegerValue()];
		var lowerValue = context.nextIntegerValue();
		var higherValue = context.nextIntegerValue();
		return new SizeExpression(operator, lowerValue, higherValue);
	}

	private static SizeMbExpression toSizeMbExpression(Context context)
	{
		var operator = RelationalExpression.Operator.values()[context.nextIntegerValue()];
		var lowerValue = context.nextIntegerValue();
		var higherValue = context.nextIntegerValue();
		return new SizeMbExpression(operator, lowerValue, higherValue);
	}

	private static NameExpression toNameExpression(Context context)
	{
		var operator = StringExpression.Operator.values()[context.nextIntegerValue()];
		var caseSensitive = context.nextIntegerValue() == 0;
		var stringsSize = context.nextIntegerValue();
		var sb = new StringJoiner(" ");

		while (stringsSize-- > 0)
		{
			sb.add(context.nextStringValue());
		}
		return new NameExpression(operator, sb.toString(), caseSensitive);
	}

	private static PathExpression toPathExpression(Context context)
	{
		var operator = StringExpression.Operator.values()[context.nextIntegerValue()];
		var caseSensitive = context.nextIntegerValue() == 0;
		var stringsSize = context.nextIntegerValue();
		var sb = new StringJoiner(" ");

		while (stringsSize-- > 0)
		{
			sb.add(context.nextStringValue());
		}
		return new PathExpression(operator, sb.toString(), caseSensitive);
	}

	private static ExtensionExpression toExtensionExpression(Context context)
	{
		var operator = StringExpression.Operator.values()[context.nextIntegerValue()];
		var caseSensitive = context.nextIntegerValue() == 0;
		var stringsSize = context.nextIntegerValue();
		var sb = new StringJoiner(" ");

		while (stringsSize-- > 0)
		{
			sb.add(context.nextStringValue());
		}
		return new ExtensionExpression(operator, sb.toString(), caseSensitive);
	}

	private static HashExpression toHashExpression(Context context)
	{
		var operator = StringExpression.Operator.values()[context.nextIntegerValue()];
		context.skipIntegerValue(); // No case sensitivity needed
		var stringsSize = context.nextIntegerValue();
		var sb = new StringJoiner(" ");

		while (stringsSize-- > 0)
		{
			sb.add(context.nextStringValue());
		}
		return new HashExpression(operator, sb.toString());
	}

	private static CompoundExpression toCompoundExpression(Context context)
	{
		var operator = CompoundExpression.Operator.values()[context.nextIntegerValue()];
		var leftCompound = toExpression(context);
		var rightCompound = toExpression(context);

		return new CompoundExpression(operator, leftCompound, rightCompound);
	}
}
