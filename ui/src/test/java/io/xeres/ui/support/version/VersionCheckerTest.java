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

package io.xeres.ui.support.version;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionCheckerTest
{
	@Test
	void IsNewVersion_Major_OK()
	{
		assertTrue(VersionChecker.isVersionMoreRecent("1.0.0", "0.1.1"));
	}

	@Test
	void IsNewVersion_Minor_OK()
	{
		assertTrue(VersionChecker.isVersionMoreRecent("1.1.0", "1.0.5"));
	}

	@Test
	void IsNewVersion_Patch_OK()
	{
		assertTrue(VersionChecker.isVersionMoreRecent("1.1.1", "1.1.0"));
	}

	@Test
	void IsNewVersion_Same_OK()
	{
		assertFalse(VersionChecker.isVersionMoreRecent("1.1.1", "1.1.1"));
	}
}