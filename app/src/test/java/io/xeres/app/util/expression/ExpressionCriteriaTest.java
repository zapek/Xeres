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

import io.xeres.app.database.model.file.FileFakes;
import io.xeres.app.database.repository.FileRepository;
import io.xeres.app.service.file.FileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.web.WebAppConfiguration;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.UseMainMethod.ALWAYS;

@SpringBootTest(args = "--no-gui", useMainMethod = ALWAYS)
@WebAppConfiguration // see https://stackoverflow.com/questions/73575360/attribute-javax-websocket-server-servercontainer-not-found-in-servletcontext-w
class ExpressionCriteriaTest
{
	@Autowired
	private FileService fileService;

	@Autowired
	private FileRepository fileRepository;

	@Test
	void Name()
	{
		var file = FileFakes.createFile("The Great Race.mkv");
		fileRepository.save(file);

		var expressionEqualsOk = new NameExpression(StringExpression.Operator.EQUALS, "The Great Race.mkv", true);
		var expressionEqualsNoCaseOk = new NameExpression(StringExpression.Operator.EQUALS, "the great race.mkv", false);
		var expressionEqualsCaseFail = new NameExpression(StringExpression.Operator.EQUALS, "the great race.mkv", true);
		var expressionEqualsFail = new NameExpression(StringExpression.Operator.EQUALS, "The Great Race", false);

		var expressionAllOk = new NameExpression(StringExpression.Operator.CONTAINS_ALL, "Race Great", true);
		var expressionAllNoCaseOk = new NameExpression(StringExpression.Operator.CONTAINS_ALL, "race great", false);
		var expressionAllCaseFail = new NameExpression(StringExpression.Operator.CONTAINS_ALL, "race great", true);
		var expressionAllFail = new NameExpression(StringExpression.Operator.CONTAINS_ALL, "Race Great Foo", false);

		var expressionAnyOk = new NameExpression(StringExpression.Operator.CONTAINS_ANY, "Race Stuff", true);
		var expressionAnyNoCaseOk = new NameExpression(StringExpression.Operator.CONTAINS_ANY, "race stuff", false);
		var expressionAnyCaseFail = new NameExpression(StringExpression.Operator.CONTAINS_ANY, "race stuff", true);
		var expressionAnyFail = new NameExpression(StringExpression.Operator.CONTAINS_ANY, "Foo Bar Plop", false);

		assertEquals(file.getName(), fileService.searchFiles(List.of(expressionEqualsOk)).getFirst().getName());
		assertEquals(file.getName(), fileService.searchFiles(List.of(expressionEqualsNoCaseOk)).getFirst().getName());
		assertTrue(fileService.searchFiles(List.of(expressionEqualsCaseFail)).isEmpty());
		assertTrue(fileService.searchFiles(List.of(expressionEqualsFail)).isEmpty());

		assertEquals(file.getName(), fileService.searchFiles(List.of(expressionAllOk)).getFirst().getName());
		assertEquals(file.getName(), fileService.searchFiles(List.of(expressionAllNoCaseOk)).getFirst().getName());
		assertTrue(fileService.searchFiles(List.of(expressionAllCaseFail)).isEmpty());
		assertTrue(fileService.searchFiles(List.of(expressionAllFail)).isEmpty());

		assertEquals(file.getName(), fileService.searchFiles(List.of(expressionAnyOk)).getFirst().getName());
		assertEquals(file.getName(), fileService.searchFiles(List.of(expressionAnyNoCaseOk)).getFirst().getName());
		assertTrue(fileService.searchFiles(List.of(expressionAnyCaseFail)).isEmpty());
		assertTrue(fileService.searchFiles(List.of(expressionAnyFail)).isEmpty());

		fileRepository.delete(file);
	}

	@Test
	void Path()
	{
		var parent = FileFakes.createFile("Movies");
		fileRepository.save(parent);
		var file = FileFakes.createFile("The Great Race.mkv", parent);
		fileRepository.save(file);

		var expressionNotSupported = new PathExpression(StringExpression.Operator.EQUALS, "Movies", false);

		assertTrue(fileService.searchFiles(List.of(expressionNotSupported)).isEmpty());

		fileRepository.delete(parent);
		fileRepository.delete(file);
	}

	@Test
	void Extension()
	{
		var file = FileFakes.createFile("The Empty Bin.EXE");
		fileRepository.save(file);

		var expressionEqualsOk = new ExtensionExpression(StringExpression.Operator.EQUALS, "EXE", true);
		var expressionEqualsNoCaseOk = new ExtensionExpression(StringExpression.Operator.EQUALS, "exe", false);
		var expressionEqualsCaseFail = new ExtensionExpression(StringExpression.Operator.EQUALS, "exe", true);
		var expressionEqualsFail = new ExtensionExpression(StringExpression.Operator.EQUALS, "bin", false);

		assertEquals(file.getName(), fileService.searchFiles(List.of(expressionEqualsOk)).getFirst().getName());
		assertEquals(file.getName(), fileService.searchFiles(List.of(expressionEqualsNoCaseOk)).getFirst().getName());
		assertTrue(fileService.searchFiles(List.of(expressionEqualsCaseFail)).isEmpty());
		assertTrue(fileService.searchFiles(List.of(expressionEqualsFail)).isEmpty());

		fileRepository.delete(file);
	}

//	@Test
//	void ExpressionCriteria_Hash()
//	{
//		var file = FileFakes.createFile("Stuff", 1024, Instant.now(), Sha1SumFakes.createSha1Sum());
//		fileRepository.save(file);
//
//		var expressionEqualsOk = new HashExpression(StringExpression.Operator.EQUALS, file.getHash().toString());
//
//		assertEquals(file.getName(), fileService.searchFiles(List.of(expressionEqualsOk)).getFirst().getName());
//
//	    fileRepository.delete(file);
//	}

	@Test
	void Date()
	{
		var file = FileFakes.createFile("Foobar", 1024, Instant.now().truncatedTo(ChronoUnit.SECONDS));
		fileRepository.save(file);

		var expressionEqualsOk = new DateExpression(RelationalExpression.Operator.EQUALS, (int) file.getModified().getEpochSecond(), 0);
		var expressionInRange = new DateExpression(RelationalExpression.Operator.IN_RANGE, (int) file.getModified().getEpochSecond() - 1, (int) file.getModified().getEpochSecond() + 1);

		assertEquals(file.getName(), fileService.searchFiles(List.of(expressionEqualsOk)).getFirst().getName());
		assertEquals(file.getName(), fileService.searchFiles(List.of(expressionInRange)).getFirst().getName());

		fileRepository.delete(file);
	}

	@Test
	void Size()
	{
		var file = FileFakes.createFile("foobar", 1024);
		fileRepository.save(file);

		var expressionEqualsOk = new SizeExpression(RelationalExpression.Operator.EQUALS, 1024, 0);
		var expressionEqualsFail = new SizeExpression(RelationalExpression.Operator.EQUALS, 1025, 0);

		var expressionGreaterThanOrEqualsOk = new SizeExpression(RelationalExpression.Operator.GREATER_THAN_OR_EQUALS, 1024, 0);
		var expressionGreaterThanOrEqualsFail = new SizeExpression(RelationalExpression.Operator.GREATER_THAN_OR_EQUALS, 1023, 0);

		var expressionGreaterThanOk = new SizeExpression(RelationalExpression.Operator.GREATER_THAN, 1025, 0);
		var expressionGreaterThanFail = new SizeExpression(RelationalExpression.Operator.GREATER_THAN, 1024, 0);

		var expressionLesserThanOrEqualsOk = new SizeExpression(RelationalExpression.Operator.LESSER_THAN_OR_EQUALS, 1024, 0);
		var expressionLesserThanOrEqualsFail = new SizeExpression(RelationalExpression.Operator.LESSER_THAN_OR_EQUALS, 1025, 0);

		var expressionLesserThanOk = new SizeExpression(RelationalExpression.Operator.LESSER_THAN, 1023, 0);
		var expressionLesserThanFail = new SizeExpression(RelationalExpression.Operator.LESSER_THAN, 1024, 0);

		var expressionInRangeOk = new SizeExpression(RelationalExpression.Operator.IN_RANGE, 1023, 1025);
		var expressionInRangeFail = new SizeExpression(RelationalExpression.Operator.IN_RANGE, 1025, 1026);

		assertEquals(file.getName(), fileService.searchFiles(List.of(expressionEqualsOk)).getFirst().getName());
		assertTrue(fileService.searchFiles(List.of(expressionEqualsFail)).isEmpty());

		assertEquals(file.getName(), fileService.searchFiles(List.of(expressionGreaterThanOrEqualsOk)).getFirst().getName());
		assertTrue(fileService.searchFiles(List.of(expressionGreaterThanOrEqualsFail)).isEmpty());

		assertEquals(file.getName(), fileService.searchFiles(List.of(expressionGreaterThanOk)).getFirst().getName());
		assertTrue(fileService.searchFiles(List.of(expressionGreaterThanFail)).isEmpty());

		assertEquals(file.getName(), fileService.searchFiles(List.of(expressionLesserThanOrEqualsOk)).getFirst().getName());
		assertTrue(fileService.searchFiles(List.of(expressionLesserThanOrEqualsFail)).isEmpty());

		assertEquals(file.getName(), fileService.searchFiles(List.of(expressionLesserThanOk)).getFirst().getName());
		assertTrue(fileService.searchFiles(List.of(expressionLesserThanFail)).isEmpty());

		assertEquals(file.getName(), fileService.searchFiles(List.of(expressionInRangeOk)).getFirst().getName());
		assertTrue(fileService.searchFiles(List.of(expressionInRangeFail)).isEmpty());

		fileRepository.delete(file);
	}

	@Test
	void SizeMb()
	{
		var file = FileFakes.createFile("foobar", 1_000_000_000_000L);
		fileRepository.save(file);

		var expressionEqualsOk = new SizeMbExpression(RelationalExpression.Operator.EQUALS, (int) (1_000_000_000_000L >> 20), 0);
		var expressionEqualsFail = new SizeMbExpression(RelationalExpression.Operator.EQUALS, (int) (1_000_001_000_000L >> 20), 0);

		var expressionGreaterThanOrEqualsOk = new SizeMbExpression(RelationalExpression.Operator.GREATER_THAN_OR_EQUALS, (int) (1_000_000_000_000L >> 20), 0);
		var expressionGreaterThanOrEqualsFail = new SizeMbExpression(RelationalExpression.Operator.GREATER_THAN_OR_EQUALS, (int) (900_000_000_000L >> 20), 0);

		var expressionGreaterThanOk = new SizeMbExpression(RelationalExpression.Operator.GREATER_THAN, (int) (1_000_001_000_000L >> 20), 0);
		var expressionGreaterThanFail = new SizeMbExpression(RelationalExpression.Operator.GREATER_THAN, (int) (999_000_000_000L >> 20), 0); // Note that 1_000_000_000_000 should fail, but it doesn't because of the lost precision

		var expressionLesserThanOrEqualsOk = new SizeMbExpression(RelationalExpression.Operator.LESSER_THAN_OR_EQUALS, (int) (1_000_000_000_000L >> 20), 0);
		var expressionLesserThanOrEqualsFail = new SizeMbExpression(RelationalExpression.Operator.LESSER_THAN_OR_EQUALS, (int) (1_000_001_000_000L >> 20), 0);

		var expressionLesserThanOk = new SizeMbExpression(RelationalExpression.Operator.LESSER_THAN, (int) (1_000_000_000_000L >> 20), 0);
		var expressionLesserThanFail = new SizeMbExpression(RelationalExpression.Operator.LESSER_THAN, (int) (1_000_001_000_000L >> 20), 0);

		var expressionInRangeOk = new SizeMbExpression(RelationalExpression.Operator.IN_RANGE, (int) (900_000_000_000L >> 20), (int) (1_000_001_000_000L >> 20));
		var expressionInRangeFail = new SizeMbExpression(RelationalExpression.Operator.IN_RANGE, (int) (1_000_001_000_000L >> 20), (int) (2_000_000_000_000L >> 20));

		assertEquals(file.getName(), fileService.searchFiles(List.of(expressionEqualsOk)).getFirst().getName());
		assertTrue(fileService.searchFiles(List.of(expressionEqualsFail)).isEmpty());

		assertEquals(file.getName(), fileService.searchFiles(List.of(expressionGreaterThanOrEqualsOk)).getFirst().getName());
		assertTrue(fileService.searchFiles(List.of(expressionGreaterThanOrEqualsFail)).isEmpty());

		assertEquals(file.getName(), fileService.searchFiles(List.of(expressionGreaterThanOk)).getFirst().getName());
		assertTrue(fileService.searchFiles(List.of(expressionGreaterThanFail)).isEmpty());

		assertEquals(file.getName(), fileService.searchFiles(List.of(expressionLesserThanOrEqualsOk)).getFirst().getName());
		assertTrue(fileService.searchFiles(List.of(expressionLesserThanOrEqualsFail)).isEmpty());

		assertEquals(file.getName(), fileService.searchFiles(List.of(expressionLesserThanOk)).getFirst().getName());
		assertTrue(fileService.searchFiles(List.of(expressionLesserThanFail)).isEmpty());

		assertEquals(file.getName(), fileService.searchFiles(List.of(expressionInRangeOk)).getFirst().getName());
		assertTrue(fileService.searchFiles(List.of(expressionInRangeFail)).isEmpty());

		fileRepository.delete(file);
	}

	@Test
	void Popularity_NotSupported()
	{
		var file = FileFakes.createFile("foobar");
		fileRepository.save(file);

		var expressionEqualsNotSupported = new PopularityExpression(RelationalExpression.Operator.EQUALS, 1, 0);

		assertTrue(fileService.searchFiles(List.of(expressionEqualsNotSupported)).isEmpty());

		fileRepository.delete(file);
	}
}
