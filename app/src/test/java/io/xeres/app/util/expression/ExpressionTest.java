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

import io.xeres.testutils.Sha1SumFakes;
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

	@Test
	void Expression_Date_OK()
	{
		var expression = new DateExpression(RelationalExpression.Operator.EQUALS, 1000, 0);
		var fileEntryCorrect = ExpressionFakes.createFileEntry("foo", 0, 1000, 0, null, null);
		var fileEntryWrong = ExpressionFakes.createFileEntry("foo", 0, 2000, 0, null, null);

		assertTrue(expression.evaluate(fileEntryCorrect));
		assertFalse(expression.evaluate(fileEntryWrong));
	}

	@Test
	void Expression_Popularity_OK()
	{
		var expression = new PopularityExpression(RelationalExpression.Operator.EQUALS, 1000, 0);
		var fileEntryCorrect = ExpressionFakes.createFileEntry("foo", 0, 0, 1000, null, null);
		var fileEntryWrong = ExpressionFakes.createFileEntry("foo", 0, 0, 2000, null, null);

		assertTrue(expression.evaluate(fileEntryCorrect));
		assertFalse(expression.evaluate(fileEntryWrong));
	}

	@Test
	void Expression_SizeMb_OK()
	{
		var expression = new SizeMbExpression(RelationalExpression.Operator.EQUALS, (int) (1_000_000_000_000L >> 20), 0);
		var fileEntryCorrect1 = ExpressionFakes.createFileEntry("foo", 1_000_000_000_000L, 0, 1000, null, null);
		var fileEntryCorrect2 = ExpressionFakes.createFileEntry("foo", 1_000_000_000_001L, 0, 1000, null, null);
		var fileEntryWrong = ExpressionFakes.createFileEntry("foo", 1_000_001_000_000L, 0, 1000, null, null);

		assertTrue(expression.evaluate(fileEntryCorrect1));
		assertTrue(expression.evaluate(fileEntryCorrect2));
		assertFalse(expression.evaluate(fileEntryWrong));
	}

	@Test
	void Expression_Path_OK()
	{
		var expression = new PathExpression(StringExpression.Operator.CONTAINS_ANY, "coolstuff", false);
		var fileEntryCorrect = ExpressionFakes.createFileEntry("foobar", 0, 0, 0, "C:\\coolstuff\\bla", null);
		var fileEntryWrong = ExpressionFakes.createFileEntry("foobar", 0, 0, 0, "C:\\nothing\\bla", null);

		assertTrue(expression.evaluate(fileEntryCorrect));
		assertFalse(expression.evaluate(fileEntryWrong));
	}

	@Test
	void Expression_Extension_OK()
	{
		var expression = new ExtensionExpression(StringExpression.Operator.CONTAINS_ANY, "exe com", false);
		var fileEntryCorrect1 = ExpressionFakes.createFileEntry("foobar.exe", 0, 0, 0, null, null);
		var fileEntryCorrect2 = ExpressionFakes.createFileEntry("foobar.com", 0, 0, 0, null, null);
		var fileEntryWrong = ExpressionFakes.createFileEntry("foobar.bin", 0, 0, 0, null, null);

		assertTrue(expression.evaluate(fileEntryCorrect1));
		assertTrue(expression.evaluate(fileEntryCorrect2));
		assertFalse(expression.evaluate(fileEntryWrong));
	}

	@Test
	void Expression_Hash_OK()
	{
		var hash1 = Sha1SumFakes.createSha1Sum();
		var hash2 = Sha1SumFakes.createSha1Sum();
		var expression = new HashExpression(StringExpression.Operator.EQUALS, hash1.toString());
		var fileEntryCorrect = ExpressionFakes.createFileEntry("foobar", 0, 0, 0, null, hash1);
		var fileEntryWrong = ExpressionFakes.createFileEntry("foobar.bin", 0, 0, 0, null, hash2);

		assertTrue(expression.evaluate(fileEntryCorrect));
		assertFalse(expression.evaluate(fileEntryWrong));
	}

	@Test
	void Expression_Compound_AND()
	{
		var left = new NameExpression(StringExpression.Operator.EQUALS, "foo", false);
		var right = new SizeExpression(RelationalExpression.Operator.EQUALS, 1000, 0);
		var compound = new CompoundExpression(CompoundExpression.Operator.AND, left, right);
		var fileEntryCorrect = ExpressionFakes.createFileEntry("foo", 1000, 0, 0, null, null);
		var fileEntryWrong = ExpressionFakes.createFileEntry("foo", 1001, 0, 0, null, null);

		assertTrue(compound.evaluate(fileEntryCorrect));
		assertFalse(compound.evaluate(fileEntryWrong));
	}

	@Test
	void Expression_Compound_OR()
	{
		var left = new NameExpression(StringExpression.Operator.EQUALS, "foo", false);
		var right = new SizeExpression(RelationalExpression.Operator.EQUALS, 1000, 0);
		var compound = new CompoundExpression(CompoundExpression.Operator.OR, left, right);
		var fileEntryCorrectAnd = ExpressionFakes.createFileEntry("foo", 1000, 0, 0, null, null);
		var fileEntryCorrectOr = ExpressionFakes.createFileEntry("foo", 1001, 0, 0, null, null);
		var fileEntryWrong = ExpressionFakes.createFileEntry("bar", 1001, 0, 0, null, null);

		assertTrue(compound.evaluate(fileEntryCorrectAnd));
		assertTrue(compound.evaluate(fileEntryCorrectOr));
		assertFalse(compound.evaluate(fileEntryWrong));
	}

	@Test
	void Expression_Compound_XOR()
	{
		var left = new NameExpression(StringExpression.Operator.EQUALS, "foo", false);
		var right = new SizeExpression(RelationalExpression.Operator.EQUALS, 1000, 0);
		var compound = new CompoundExpression(CompoundExpression.Operator.XOR, left, right);
		var fileEntryCorrectOr = ExpressionFakes.createFileEntry("foo", 1001, 0, 0, null, null);
		var fileEntryWrongAnd = ExpressionFakes.createFileEntry("foo", 1000, 0, 0, null, null);
		var fileEntryWrongBoth = ExpressionFakes.createFileEntry("bar", 1001, 0, 0, null, null);

		assertTrue(compound.evaluate(fileEntryCorrectOr));
		assertFalse(compound.evaluate(fileEntryWrongAnd));
		assertFalse(compound.evaluate(fileEntryWrongBoth));
	}
}
