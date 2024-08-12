package io.xeres.app.util;

import io.xeres.common.util.OsUtils;
import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OsUtilsTest
{
	@Test
	void OsUtils_IsFileSystemCaseSensitive_OK()
	{
		var tempDir = System.getProperty("java.io.tmpdir");

		var isCaseSensitive = OsUtils.isFileSystemCaseSensitive(Path.of(tempDir));

		if (SystemUtils.IS_OS_WINDOWS)
		{
			assertFalse(isCaseSensitive);
		}
		else if (SystemUtils.IS_OS_LINUX)
		{
			assertTrue(isCaseSensitive);
		}
		else if (SystemUtils.IS_OS_MAC)
		{
			assertFalse(isCaseSensitive);
		}
		// Don't care on other operating systems
	}
}