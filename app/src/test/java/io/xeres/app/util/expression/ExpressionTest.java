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

import io.xeres.app.database.model.file.FileFakes;
import io.xeres.testutils.Sha1SumFakes;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpressionTest
{
	@Test
	void Name_Equals()
	{
		var expression = new NameExpression(StringExpression.Operator.EQUALS, "foobar", false);
		var fileCorrect = FileFakes.createFile("foobar");
		var fileWrong = FileFakes.createFile("blahblah");

		assertTrue(expression.evaluate(fileCorrect));
		assertFalse(expression.evaluate(fileWrong));
	}

	@Test
	void Name_Equals_CaseSensitive()
	{
		var expression = new NameExpression(StringExpression.Operator.EQUALS, "foobar", true);
		var fileCorrect = FileFakes.createFile("foobar");
		var fileWrong = FileFakes.createFile("FooBar");

		assertTrue(expression.evaluate(fileCorrect));
		assertFalse(expression.evaluate(fileWrong));
	}

	@Test
	void Name_ContainsAll()
	{
		var expression = new NameExpression(StringExpression.Operator.CONTAINS_ALL, "foo bar plop", false);
		var fileCorrect = FileFakes.createFile("foo bar plop");
		var fileWrong = FileFakes.createFile("foo bar");

		assertTrue(expression.evaluate(fileCorrect));
		assertFalse(expression.evaluate(fileWrong));
	}

	@Test
	void Name_ContainsAll_CaseSensitive()
	{
		var expression = new NameExpression(StringExpression.Operator.CONTAINS_ALL, "foo bar plop", true);
		var fileCorrect = FileFakes.createFile("foo bar plop");
		var fileWrong = FileFakes.createFile("Foo bar plop");

		assertTrue(expression.evaluate(fileCorrect));
		assertFalse(expression.evaluate(fileWrong));
	}

	@Test
	void Name_ContainsAny()
	{
		var expression = new NameExpression(StringExpression.Operator.CONTAINS_ANY, "foo bar plop", false);
		var fileCorrect1 = FileFakes.createFile("foo");
		var fileCorrect2 = FileFakes.createFile("bar");
		var fileCorrect3 = FileFakes.createFile("plop");
		var fileWrong = FileFakes.createFile("none niet nada");

		assertTrue(expression.evaluate(fileCorrect1));
		assertTrue(expression.evaluate(fileCorrect2));
		assertTrue(expression.evaluate(fileCorrect3));
		assertFalse(expression.evaluate(fileWrong));
	}

	@Test
	void Name_ContainsAny_CaseSensitive()
	{
		var expression = new NameExpression(StringExpression.Operator.CONTAINS_ANY, "foo bar plop", true);
		var fileCorrect1 = FileFakes.createFile("foo");
		var fileWrong1 = FileFakes.createFile("Bar");
		var fileWrong2 = FileFakes.createFile("Plop");
		var fileWrong3 = FileFakes.createFile("none niet nada");

		assertTrue(expression.evaluate(fileCorrect1));
		assertFalse(expression.evaluate(fileWrong1));
		assertFalse(expression.evaluate(fileWrong2));
		assertFalse(expression.evaluate(fileWrong3));
	}

	@Test
	void Size_Equals()
	{
		var expression = new SizeExpression(RelationalExpression.Operator.EQUALS, 1024, 0);
		var fileCorrect = FileFakes.createFile("foo", 1024);
		var fileWrong = FileFakes.createFile("foo", 512);

		assertTrue(expression.evaluate(fileCorrect));
		assertFalse(expression.evaluate(fileWrong));
	}

	@Test
	void Size_GreaterThanOrEquals()
	{
		var expression = new SizeExpression(RelationalExpression.Operator.GREATER_THAN_OR_EQUALS, 1024, 0);
		var fileCorrect1 = FileFakes.createFile("foo", 1024);
		var fileCorrect2 = FileFakes.createFile("foo", 1023);
		var fileWrong = FileFakes.createFile("foo", 1025);

		assertTrue(expression.evaluate(fileCorrect1));
		assertTrue(expression.evaluate(fileCorrect2));
		assertFalse(expression.evaluate(fileWrong));
	}

	@Test
	void Size_GreaterThan()
	{
		var expression = new SizeExpression(RelationalExpression.Operator.GREATER_THAN, 1024, 0);
		var fileCorrect1 = FileFakes.createFile("foo", 1023);
		var fileWrong1 = FileFakes.createFile("foo", 1024);
		var fileWrong2 = FileFakes.createFile("foo", 1025);

		assertTrue(expression.evaluate(fileCorrect1));
		assertFalse(expression.evaluate(fileWrong1));
		assertFalse(expression.evaluate(fileWrong2));
	}

	@Test
	void Size_LesserThanOrEquals()
	{
		var expression = new SizeExpression(RelationalExpression.Operator.LESSER_THAN_OR_EQUALS, 1024, 0);
		var fileCorrect1 = FileFakes.createFile("foo", 1024);
		var fileCorrect2 = FileFakes.createFile("foo", 1025);
		var fileWrong = FileFakes.createFile("foo", 512);

		assertTrue(expression.evaluate(fileCorrect1));
		assertTrue(expression.evaluate(fileCorrect2));
		assertFalse(expression.evaluate(fileWrong));
	}

	@Test
	void Size_LesserThan()
	{
		var expression = new SizeExpression(RelationalExpression.Operator.LESSER_THAN, 1024, 0);
		var fileCorrect1 = FileFakes.createFile("foo", 1025);
		var fileWrong1 = FileFakes.createFile("foo", 1024);
		var fileWrong2 = FileFakes.createFile("foo", 512);

		assertTrue(expression.evaluate(fileCorrect1));
		assertFalse(expression.evaluate(fileWrong1));
		assertFalse(expression.evaluate(fileWrong2));
	}

	@Test
	void Size_InRange()
	{
		var expression = new SizeExpression(RelationalExpression.Operator.IN_RANGE, 1024, 2048);
		var fileCorrect1 = FileFakes.createFile("foo", 1024);
		var fileCorrect2 = FileFakes.createFile("foo", 2048);
		var fileCorrect3 = FileFakes.createFile("foo", 1536);
		var fileWrong1 = FileFakes.createFile("foo", 1023);
		var fileWrong2 = FileFakes.createFile("foo", 2049);

		assertTrue(expression.evaluate(fileCorrect1));
		assertTrue(expression.evaluate(fileCorrect2));
		assertTrue(expression.evaluate(fileCorrect3));
		assertFalse(expression.evaluate(fileWrong1));
		assertFalse(expression.evaluate(fileWrong2));
	}

	@Test
	void Date()
	{
		var expression = new DateExpression(RelationalExpression.Operator.EQUALS, 1000, 0);
		var fileCorrect = FileFakes.createFile("foo", 1024, Instant.ofEpochSecond(1000));
		var fileWrong = FileFakes.createFile("foo", 1024, Instant.ofEpochSecond(2000));

		assertTrue(expression.evaluate(fileCorrect));
		assertFalse(expression.evaluate(fileWrong));
	}

	@Test
	void Popularity()
	{
		// Popularity is not implemented (there's no "popularity" in a local file), so it's always zero
		var expression1 = new PopularityExpression(RelationalExpression.Operator.EQUALS, 1, 0);
		var expression2 = new PopularityExpression(RelationalExpression.Operator.EQUALS, 0, 0);
		var file = FileFakes.createFile("foo");

		assertFalse(expression1.evaluate(file));
		assertTrue(expression2.evaluate(file));
	}

	@Test
	void SizeMb()
	{
		var expression = new SizeMbExpression(RelationalExpression.Operator.EQUALS, (int) (1_000_000_000_000L >> 20), 0);
		var fileCorrect1 = FileFakes.createFile("foo", 1_000_000_000_000L);
		var fileCorrect2 = FileFakes.createFile("foo", 1_000_000_000_001L);
		var fileWrong = FileFakes.createFile("foo", 1_000_001_000_000L);

		assertTrue(expression.evaluate(fileCorrect1));
		assertTrue(expression.evaluate(fileCorrect2));
		assertFalse(expression.evaluate(fileWrong));
	}

	@Test
	void Path()
	{
		// Path is not implemented because it's very difficult to do for no real gain
		var expression = new PathExpression(StringExpression.Operator.CONTAINS_ANY, "coolstuff", false);
		var file = FileFakes.createFile("foo");

		assertFalse(expression.evaluate(file));
	}

	@Test
	void Extension()
	{
		var expression = new ExtensionExpression(StringExpression.Operator.CONTAINS_ANY, "exe com", false);
		var fileCorrect1 = FileFakes.createFile("foobar.exe");
		var fileCorrect2 = FileFakes.createFile("foobar.com");
		var fileWrong1 = FileFakes.createFile("foobar.bin");
		var fileWrong2 = FileFakes.createFile("The.Exe.bin");

		assertTrue(expression.evaluate(fileCorrect1));
		assertTrue(expression.evaluate(fileCorrect2));
		assertFalse(expression.evaluate(fileWrong1));
		assertFalse(expression.evaluate(fileWrong2));
	}

	@Test
	void Hash()
	{
		var hash1 = Sha1SumFakes.createSha1Sum();
		var hash2 = Sha1SumFakes.createSha1Sum();
		var expression = new HashExpression(StringExpression.Operator.EQUALS, hash1.toString());
		var fileCorrect = FileFakes.createFile("foobar", 0, null, hash1);
		var fileWrong = FileFakes.createFile("foobar", 0, null, hash2);

		assertTrue(expression.evaluate(fileCorrect));
		assertFalse(expression.evaluate(fileWrong));
	}

	@Test
	void Compound_AND()
	{
		var left = new NameExpression(StringExpression.Operator.EQUALS, "foo", false);
		var right = new SizeExpression(RelationalExpression.Operator.EQUALS, 1000, 0);
		var compound = new CompoundExpression(CompoundExpression.Operator.AND, left, right);
		var fileCorrect = FileFakes.createFile("foo", 1000);
		var fileWrong = FileFakes.createFile("foo", 1001);

		assertTrue(compound.evaluate(fileCorrect));
		assertFalse(compound.evaluate(fileWrong));
	}

	@Test
	void Compound_OR()
	{
		var left = new NameExpression(StringExpression.Operator.EQUALS, "foo", false);
		var right = new SizeExpression(RelationalExpression.Operator.EQUALS, 1000, 0);
		var compound = new CompoundExpression(CompoundExpression.Operator.OR, left, right);
		var fileCorrectAnd = FileFakes.createFile("foo", 1000);
		var fileCorrectOr = FileFakes.createFile("foo", 1001);
		var fileWrong = FileFakes.createFile("bar", 1001);

		assertTrue(compound.evaluate(fileCorrectAnd));
		assertTrue(compound.evaluate(fileCorrectOr));
		assertFalse(compound.evaluate(fileWrong));
	}

	@Test
	void Compound_XOR()
	{
		var left = new NameExpression(StringExpression.Operator.EQUALS, "foo", false);
		var right = new SizeExpression(RelationalExpression.Operator.EQUALS, 1000, 0);
		var compound = new CompoundExpression(CompoundExpression.Operator.XOR, left, right);
		var fileCorrectOr = FileFakes.createFile("foo", 1001);
		var fileWrongAnd = FileFakes.createFile("foo", 1000);
		var fileWrongBoth = FileFakes.createFile("bar", 1001);

		assertTrue(compound.evaluate(fileCorrectOr));
		assertFalse(compound.evaluate(fileWrongAnd));
		assertFalse(compound.evaluate(fileWrongBoth));
	}
}
