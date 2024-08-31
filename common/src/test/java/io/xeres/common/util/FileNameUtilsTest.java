package io.xeres.common.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileNameUtilsTest
{
	@ParameterizedTest
	@CsvSource({
			"foo.jpg,foo (1).jpg",
			"foo,foo (1)",
			"foo.tar.gz,foo (1).tar.gz",
			"foo.bla.tgz,foo.bla (1).tgz",
			"foo.blabla.gz,foo.blabla (1).gz",
			"foo.bar.plop.tar.gz,foo.bar.plop (1).tar.gz",
			"foo (1).jpg,foo (2).jpg",
			"foo (9).jpg,foo (10).jpg",
			"foo (bar).jpg,foo (bar) (1).jpg",
			"foo (1)(2).jpg,foo (1)(3).jpg",
			"foo ().jpg,foo () (1).jpg"
	})
	void Rename_Various_Success(String input, String expected)
	{
		var result = FileNameUtils.rename(input);
		assertEquals(expected, result);
	}

	@Test
	void Rename_Empty_ThrowsException()
	{
		assertThrows(IllegalArgumentException.class, () -> FileNameUtils.rename(""));
	}
}