package io.xeres.common;

import io.xeres.testutils.TestUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AppNameTest
{
	@Test
	void AppName_NoInstance_OK() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(AppName.class);
	}

	@Test
	void AppName_Name_NotBlank()
	{
		assertTrue(StringUtils.isNotBlank(AppName.NAME));
	}
}
