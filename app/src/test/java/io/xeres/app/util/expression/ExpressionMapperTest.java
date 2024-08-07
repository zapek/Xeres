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
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExpressionMapperTest
{
	@Test
	void ExpressionMapper_Name()
	{
		List<Byte> tokens = new ArrayList<>();
		List<Integer> ints = new ArrayList<>();
		List<String> strings = new ArrayList<>();

		tokens.add((byte) 4); // Name
		ints.add(1); // Contains all
		ints.add(1); // Case-insensitive
		ints.add(2); // 2 words
		strings.add("foo"); // word 1
		strings.add("bar"); // word 2

		var item = new TurtleRegExpSearchRequestItem(tokens, ints, strings);

		var expressions = ExpressionMapper.toExpressions(item);
		assertEquals(1, expressions.size());
		var expression = expressions.getFirst();
		assertInstanceOf(NameExpression.class, expression);
		var fileEntryValid = ExpressionFakes.createFileEntry("foo bar", 0, 0, 0, null, null);
		var fileEntryWrong = ExpressionFakes.createFileEntry("foo", 0, 0, 0, null, null);
		assertTrue(expression.evaluate(fileEntryValid));
		assertFalse(expression.evaluate(fileEntryWrong));
	}

	@Test
	void ExpressionMapper_Compound_NameAndSize()
	{
		List<Byte> tokens = new ArrayList<>();
		List<Integer> ints = new ArrayList<>();
		List<String> strings = new ArrayList<>();

		tokens.add((byte) 7); // Compound
		ints.add(0); // And

		tokens.add((byte) 4); // Name
		ints.add(2); // Equals
		ints.add(1); // Case-insensitive
		ints.add(1); // 1 word
		strings.add("foo"); // word 1

		tokens.add((byte) 2); // Size
		ints.add(5); // In range
		ints.add(1024); // Min value
		ints.add(2048); // Max value

		var item = new TurtleRegExpSearchRequestItem(tokens, ints, strings);

		var expressions = ExpressionMapper.toExpressions(item);
		assertEquals(1, expressions.size());
		var expression = expressions.getFirst();
		assertInstanceOf(CompoundExpression.class, expression);
		var fileEntryValid = ExpressionFakes.createFileEntry("foo", 1500, 0, 0, null, null);
		var fileEntryWrong1 = ExpressionFakes.createFileEntry("bar", 1500, 0, 0, null, null);
		var fileEntryWrong2 = ExpressionFakes.createFileEntry("foo", 3000, 0, 0, null, null);
		var fileEntryWrong3 = ExpressionFakes.createFileEntry("bar", 3000, 0, 0, null, null);
		assertTrue(expression.evaluate(fileEntryValid));
		assertFalse(expression.evaluate(fileEntryWrong1));
		assertFalse(expression.evaluate(fileEntryWrong2));
		assertFalse(expression.evaluate(fileEntryWrong3));
	}

	@Test
	void ExpressionMapper_Compound_RecursiveProtection()
	{
		List<Byte> tokens = new ArrayList<>();
		List<Integer> ints = new ArrayList<>();
		List<String> strings = new ArrayList<>();

		tokens.add((byte) 7); // Compound
		ints.add(0); // And

		tokens.add((byte) 7); // Compound
		ints.add(0); // And

		var item = new TurtleRegExpSearchRequestItem(tokens, ints, strings);

		assertThrows(IllegalStateException.class, () -> ExpressionMapper.toExpressions(item));
	}

	@Test
	void ExpressionMapper_Linearize()
	{
		var nameExpression = new NameExpression(StringExpression.Operator.EQUALS, "foo", false);
		var sizeExpression = new SizeExpression(RelationalExpression.Operator.IN_RANGE, 1024, 2048);
		var compoundExpression = new CompoundExpression(CompoundExpression.Operator.AND, nameExpression, sizeExpression);

		List<Byte> tokens = new ArrayList<>();
		List<Integer> ints = new ArrayList<>();
		List<String> strings = new ArrayList<>();
		compoundExpression.linearize(tokens, ints, strings);

		assertEquals(3, tokens.size());
		assertEquals((byte) 7, tokens.getFirst()); // Compound
		assertEquals((byte) 4, tokens.get(1)); // Name
		assertEquals((byte) 2, tokens.get(2)); // Size
		assertEquals(7, ints.size());
		assertEquals(0, ints.getFirst()); // AND
		assertEquals(2, ints.get(1)); // Equals
		assertEquals(1, ints.get(2)); // Ignore case
		assertEquals(1, ints.get(3)); // 1 string
		assertEquals(5, ints.get(4)); // In range
		assertEquals(1024, ints.get(5)); // low value
		assertEquals(2048, ints.get(6)); // high value
		assertEquals(1, strings.size());
		assertEquals("foo", strings.getFirst()); // 1 string
	}
}