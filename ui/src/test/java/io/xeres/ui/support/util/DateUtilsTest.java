/*
 * Copyright (c) 2026 by David Gerber - https://zapek.com
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

package io.xeres.ui.support.util;

import io.xeres.testutils.TestUtils;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class DateUtilsTest
{
	@Test
	void Instance_Throws() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(DateUtils.class);
	}

	@Test
	void FormatDateTime_ValidInstant_FormatsCorrectly()
	{
		var instant = Instant.parse("2026-01-06T21:39:00Z");

		var result = DateUtils.formatDateTime(instant, "unset");

		// The formatter uses system default zone, so just verify it's not "unset" and contains expected parts
		assertNotEquals("unset", result);
		assertTrue(result.contains("2026"));
	}

	@Test
	void FormatDateTime_Null_ReturnsUnset()
	{
		assertEquals("unset", DateUtils.formatDateTime(null, "unset"));
	}

	@Test
	void FormatDateTime_Epoch_ReturnsUnset()
	{
		assertEquals("unset", DateUtils.formatDateTime(Instant.EPOCH, "unset"));
	}

	@Test
	void FormatDateTime_AfterEpoch_Formats()
	{
		var instant = Instant.EPOCH.plusSeconds(1);

		var result = DateUtils.formatDateTime(instant, "unset");

		assertNotEquals("unset", result);
	}

	@Test
	void FormatDateTime_EmptyUnset_ReturnsEmpty()
	{
		assertEquals("", DateUtils.formatDateTime(null, ""));
	}

	@Test
	void Formatters_AreNotNull()
	{
		assertNotNull(DateUtils.DATE_TIME_FORMAT);
		assertNotNull(DateUtils.DATE_TIME_PRECISE_FORMAT);
		assertNotNull(DateUtils.TIME_FORMAT);
		assertNotNull(DateUtils.DATE_FORMAT);
		assertNotNull(DateUtils.TIME_PRECISE_FORMAT);
		assertNotNull(DateUtils.DATE_TIME_FILENAME_FORMAT);
	}

	@Test
	void DateTimeFormatter_FormatsCorrectly()
	{
		var instant = Instant.parse("2026-01-06T21:39:45Z");

		var dateResult = DateUtils.DATE_FORMAT.format(instant);
		var timeResult = DateUtils.TIME_FORMAT.format(instant);
		var dateTimeResult = DateUtils.DATE_TIME_FORMAT.format(instant);

		assertNotNull(dateResult);
		assertNotNull(timeResult);
		assertNotNull(dateTimeResult);
		assertFalse(dateResult.isEmpty());
		assertFalse(timeResult.isEmpty());
		assertFalse(dateTimeResult.isEmpty());
	}
}
