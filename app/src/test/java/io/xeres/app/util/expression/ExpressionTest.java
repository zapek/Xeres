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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpressionTest
{
	@Test
	void Expression_Name_Equals()
	{
		var expression = new NameExpression(StringExpression.Operator.EQUALS, "foobar", false);
		var fileEntryCorrect = ExpressionFakes.createFileEntry("foobar", 0, 0, 0, null, null);
		var fileEntryWrong = ExpressionFakes.createFileEntry("blahblah", 0, 0, 0, null, null);

		assertTrue(expression.evaluate(fileEntryCorrect));
		assertFalse(expression.evaluate(fileEntryWrong));
	}

	@Test
	void Expression_Name_Equals_CaseSensitive()
	{
		var expression = new NameExpression(StringExpression.Operator.EQUALS, "foobar", true);
		var fileEntryCorrect = ExpressionFakes.createFileEntry("foobar", 0, 0, 0, null, null);
		var fileEntryWrong = ExpressionFakes.createFileEntry("FooBar", 0, 0, 0, null, null);

		assertTrue(expression.evaluate(fileEntryCorrect));
		assertFalse(expression.evaluate(fileEntryWrong));
	}

	@Test
	void Expression_Name_ContainsAll()
	{
		var expression = new NameExpression(StringExpression.Operator.CONTAINS_ALL, "foo bar plop", false);
		var fileEntryCorrect = ExpressionFakes.createFileEntry("foo bar plop", 0, 0, 0, null, null);
		var fileEntryWrong = ExpressionFakes.createFileEntry("foo bar", 0, 0, 0, null, null);

		assertTrue(expression.evaluate(fileEntryCorrect));
		assertFalse(expression.evaluate(fileEntryWrong));
	}

	@Test
	void Expression_Name_ContainsAll_CaseSensitive()
	{
		var expression = new NameExpression(StringExpression.Operator.CONTAINS_ALL, "foo bar plop", true);
		var fileEntryCorrect = ExpressionFakes.createFileEntry("foo bar plop", 0, 0, 0, null, null);
		var fileEntryWrong = ExpressionFakes.createFileEntry("Foo bar plop", 0, 0, 0, null, null);

		assertTrue(expression.evaluate(fileEntryCorrect));
		assertFalse(expression.evaluate(fileEntryWrong));
	}

	@Test
	void Expression_Name_ContainsAny()
	{
		var expression = new NameExpression(StringExpression.Operator.CONTAINS_ANY, "foo bar plop", false);
		var fileEntryCorrect1 = ExpressionFakes.createFileEntry("foo", 0, 0, 0, null, null);
		var fileEntryCorrect2 = ExpressionFakes.createFileEntry("bar", 0, 0, 0, null, null);
		var fileEntryCorrect3 = ExpressionFakes.createFileEntry("plop", 0, 0, 0, null, null);
		var fileEntryWrong = ExpressionFakes.createFileEntry("none niet nada", 0, 0, 0, null, null);

		assertTrue(expression.evaluate(fileEntryCorrect1));
		assertTrue(expression.evaluate(fileEntryCorrect2));
		assertTrue(expression.evaluate(fileEntryCorrect3));
		assertFalse(expression.evaluate(fileEntryWrong));
	}

	@Test
	void Expression_Name_ContainsAny_CaseSensitive()
	{
		var expression = new NameExpression(StringExpression.Operator.CONTAINS_ANY, "foo bar plop", true);
		var fileEntryCorrect1 = ExpressionFakes.createFileEntry("foo", 0, 0, 0, null, null);
		var fileEntryWrong1 = ExpressionFakes.createFileEntry("Bar", 0, 0, 0, null, null);
		var fileEntryWrong2 = ExpressionFakes.createFileEntry("Plop", 0, 0, 0, null, null);
		var fileEntryWrong3 = ExpressionFakes.createFileEntry("none niet nada", 0, 0, 0, null, null);

		assertTrue(expression.evaluate(fileEntryCorrect1));
		assertFalse(expression.evaluate(fileEntryWrong1));
		assertFalse(expression.evaluate(fileEntryWrong2));
		assertFalse(expression.evaluate(fileEntryWrong3));
	}

	@Test
	void Expression_Size_Equals()
	{
		var expression = new SizeExpression(RelationalExpression.Operator.EQUALS, 1024, 0);
		var fileEntryCorrect = ExpressionFakes.createFileEntry("foo", 1024, 0, 0, null, null);
		var fileEntryWrong = ExpressionFakes.createFileEntry("foo", 512, 0, 0, null, null);

		assertTrue(expression.evaluate(fileEntryCorrect));
		assertFalse(expression.evaluate(fileEntryWrong));
	}

	@Test
	void Expression_Size_GreaterThanOrEquals()
	{
		var expression = new SizeExpression(RelationalExpression.Operator.GREATER_THAN_OR_EQUALS, 1024, 0);
		var fileEntryCorrect1 = ExpressionFakes.createFileEntry("foo", 1024, 0, 0, null, null);
		var fileEntryCorrect2 = ExpressionFakes.createFileEntry("foo", 1023, 0, 0, null, null);
		var fileEntryWrong = ExpressionFakes.createFileEntry("foo", 1025, 0, 0, null, null);

		assertTrue(expression.evaluate(fileEntryCorrect1));
		assertTrue(expression.evaluate(fileEntryCorrect2));
		assertFalse(expression.evaluate(fileEntryWrong));
	}

	@Test
	void Expression_Size_GreaterThan()
	{
		var expression = new SizeExpression(RelationalExpression.Operator.GREATER_THAN, 1024, 0);
		var fileEntryCorrect1 = ExpressionFakes.createFileEntry("foo", 1023, 0, 0, null, null);
		var fileEntryWrong1 = ExpressionFakes.createFileEntry("foo", 1024, 0, 0, null, null);
		var fileEntryWrong2 = ExpressionFakes.createFileEntry("foo", 1025, 0, 0, null, null);

		assertTrue(expression.evaluate(fileEntryCorrect1));
		assertFalse(expression.evaluate(fileEntryWrong1));
		assertFalse(expression.evaluate(fileEntryWrong2));
	}

	@Test
	void Expression_Size_LesserThanOrEquals()
	{
		var expression = new SizeExpression(RelationalExpression.Operator.LESSER_THAN_OR_EQUALS, 1024, 0);
		var fileEntryCorrect1 = ExpressionFakes.createFileEntry("foo", 1024, 0, 0, null, null);
		var fileEntryCorrect2 = ExpressionFakes.createFileEntry("foo", 1025, 0, 0, null, null);
		var fileEntryWrong = ExpressionFakes.createFileEntry("foo", 512, 0, 0, null, null);

		assertTrue(expression.evaluate(fileEntryCorrect1));
		assertTrue(expression.evaluate(fileEntryCorrect2));
		assertFalse(expression.evaluate(fileEntryWrong));
	}

	@Test
	void Expression_Size_LesserThan()
	{
		var expression = new SizeExpression(RelationalExpression.Operator.LESSER_THAN, 1024, 0);
		var fileEntryCorrect1 = ExpressionFakes.createFileEntry("foo", 1025, 0, 0, null, null);
		var fileEntryWrong1 = ExpressionFakes.createFileEntry("foo", 1024, 0, 0, null, null);
		var fileEntryWrong2 = ExpressionFakes.createFileEntry("foo", 512, 0, 0, null, null);

		assertTrue(expression.evaluate(fileEntryCorrect1));
		assertFalse(expression.evaluate(fileEntryWrong1));
		assertFalse(expression.evaluate(fileEntryWrong2));
	}

	@Test
	void Expression_Size_InRange()
	{
		var expression = new SizeExpression(RelationalExpression.Operator.IN_RANGE, 1024, 2048);
		var fileEntryCorrect1 = ExpressionFakes.createFileEntry("foo", 1024, 0, 0, null, null);
		var fileEntryCorrect2 = ExpressionFakes.createFileEntry("foo", 2048, 0, 0, null, null);
		var fileEntryCorrect3 = ExpressionFakes.createFileEntry("foo", 1536, 0, 0, null, null);
		var fileEntryWrong1 = ExpressionFakes.createFileEntry("foo", 1023, 0, 0, null, null);
		var fileEntryWrong2 = ExpressionFakes.createFileEntry("foo", 2049, 0, 0, null, null);

		assertTrue(expression.evaluate(fileEntryCorrect1));
		assertTrue(expression.evaluate(fileEntryCorrect2));
		assertTrue(expression.evaluate(fileEntryCorrect3));
		assertFalse(expression.evaluate(fileEntryWrong1));
		assertFalse(expression.evaluate(fileEntryWrong2));
	}
}
