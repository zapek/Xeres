/*
 * Copyright (c) 2024-2026 by David Gerber - https://zapek.com
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

import io.xeres.app.database.model.file.FileFakes;
import io.xeres.app.xrs.service.turtle.item.TurtleRegExpSearchRequestItem;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExpressionMapperTest
{
	@Test
	void Name()
	{
		List<Byte> tokens = new ArrayList<>();
		List<Long> uInts = new ArrayList<>();
		List<String> strings = new ArrayList<>();

		tokens.add((byte) 4); // Name
		uInts.add(1L); // Contains all
		uInts.add(1L); // Case-insensitive
		uInts.add(2L); // 2 words
		strings.add("foo"); // word 1
		strings.add("bar"); // word 2

		var item = new TurtleRegExpSearchRequestItem(tokens, uInts, strings);

		var expressions = ExpressionMapper.toExpressions(item);
		assertEquals(1, expressions.size());
		var expression = expressions.getFirst();
		assertInstanceOf(NameExpression.class, expression);
		var fileValid = FileFakes.createFile("foo bar");
		var fileInvalid = FileFakes.createFile("foo");
		assertTrue(expression.evaluate(fileValid));
		assertFalse(expression.evaluate(fileInvalid));
	}

	@Test
	void Compound_NameAndSize()
	{
		List<Byte> tokens = new ArrayList<>();
		List<Long> uInts = new ArrayList<>();
		List<String> strings = new ArrayList<>();

		tokens.add((byte) 7); // Compound
		uInts.add(0L); // And

		tokens.add((byte) 4); // Name
		uInts.add(2L); // Equals
		uInts.add(1L); // Case-insensitive
		uInts.add(1L); // 1 word
		strings.add("foo"); // word 1

		tokens.add((byte) 2); // Size
		uInts.add(5L); // In range
		uInts.add(1024L); // Min value
		uInts.add(2048L); // Max value

		var item = new TurtleRegExpSearchRequestItem(tokens, uInts, strings);

		var expressions = ExpressionMapper.toExpressions(item);
		assertEquals(1, expressions.size());
		var expression = expressions.getFirst();
		assertInstanceOf(CompoundExpression.class, expression);
		var fileEntryValid = FileFakes.createFile("foo", 1500);
		var fileEntryInvalid1 = FileFakes.createFile("bar", 1500);
		var fileEntryInvalid2 = FileFakes.createFile("foo", 3000);
		var fileEntryInvalid3 = FileFakes.createFile("bar", 3000);
		assertTrue(expression.evaluate(fileEntryValid));
		assertFalse(expression.evaluate(fileEntryInvalid1));
		assertFalse(expression.evaluate(fileEntryInvalid2));
		assertFalse(expression.evaluate(fileEntryInvalid3));
	}

	@Test
	void Linearize()
	{
		var nameExpression = new NameExpression(StringExpression.Operator.EQUALS, "foo", false);
		var sizeExpression = new SizeExpression(RelationalExpression.Operator.IN_RANGE, 1024, 2048);
		var compoundExpression = new CompoundExpression(CompoundExpression.Operator.AND, nameExpression, sizeExpression);

		List<Byte> tokens = new ArrayList<>();
		List<Long> uInts = new ArrayList<>();
		List<String> strings = new ArrayList<>();
		compoundExpression.linearize(tokens, uInts, strings);

		assertEquals(3, tokens.size());
		assertEquals((byte) 7, tokens.getFirst()); // Compound
		assertEquals((byte) 4, tokens.get(1)); // Name
		assertEquals((byte) 2, tokens.get(2)); // Size
		assertEquals(7, uInts.size());
		assertEquals(0, uInts.getFirst()); // AND
		assertEquals(2, uInts.get(1)); // Equals
		assertEquals(1, uInts.get(2)); // Ignore case
		assertEquals(1, uInts.get(3)); // 1 string
		assertEquals(5, uInts.get(4)); // In range
		assertEquals(1024, uInts.get(5)); // low value
		assertEquals(2048, uInts.get(6)); // high value
		assertEquals(1, strings.size());
		assertEquals("foo", strings.getFirst()); // 1 string
	}
}