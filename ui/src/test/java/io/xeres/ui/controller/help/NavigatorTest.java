/*
 * Copyright (c) 2025 by David Gerber - https://zapek.com
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

package io.xeres.ui.controller.help;

import io.xeres.ui.support.uri.ExternalUri;
import io.xeres.ui.support.uri.Uri;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NavigatorTest
{
	@Test
	void NavigateScenario()
	{
		List<Uri> locations = new ArrayList<>();

		var navigator = new Navigator(locations::add);

		assertFalse(navigator.backProperty.get());
		assertFalse(navigator.forwardProperty.get());

		navigator.navigate(new ExternalUri("first.md"));

		assertFalse(navigator.backProperty.get());
		assertFalse(navigator.forwardProperty.get());

		navigator.navigate(new ExternalUri("second.md"));

		assertTrue(navigator.backProperty.get());
		assertFalse(navigator.forwardProperty.get());

		navigator.navigate(new ExternalUri("third.md"));

		assertTrue(navigator.backProperty.get());
		assertFalse(navigator.forwardProperty.get());
		assertEquals(3, locations.size());
		assertEquals("first.md", locations.get(0).toString());
		assertEquals("second.md", locations.get(1).toString());
		assertEquals("third.md", locations.get(2).toString());

		navigator.navigateBackwards();

		assertTrue(navigator.backProperty.get());
		assertTrue(navigator.forwardProperty.get());
		assertEquals(4, locations.size());
		assertEquals("second.md", locations.getLast().toString());

		navigator.navigateBackwards();

		assertFalse(navigator.backProperty.get());
		assertTrue(navigator.forwardProperty.get());
		assertEquals(5, locations.size());
		assertEquals("first.md", locations.getLast().toString());

		navigator.navigateBackwards();

		// No changes
		assertFalse(navigator.backProperty.get());
		assertTrue(navigator.forwardProperty.get());
		assertEquals(5, locations.size());
		assertEquals("first.md", locations.getLast().toString());

		navigator.navigateForwards();

		assertTrue(navigator.backProperty.get());
		assertTrue(navigator.forwardProperty.get());
		assertEquals(6, locations.size());
		assertEquals("second.md", locations.getLast().toString());

		navigator.navigateForwards();

		assertTrue(navigator.backProperty.get());
		assertFalse(navigator.forwardProperty.get());
		assertEquals(7, locations.size());
		assertEquals("third.md", locations.getLast().toString());

		navigator.navigateBackwards();

		assertTrue(navigator.backProperty.get());
		assertTrue(navigator.forwardProperty.get());
		assertEquals(8, locations.size());
		assertEquals("second.md", locations.getLast().toString());

		navigator.navigate(new ExternalUri("fourth.md"));

		assertTrue(navigator.backProperty.get());
		assertFalse(navigator.forwardProperty.get());
		assertEquals(9, locations.size());
		assertEquals("fourth.md", locations.getLast().toString());

		navigator.navigateForwards();

		// No changes
		assertTrue(navigator.backProperty.get());
		assertFalse(navigator.forwardProperty.get());
		assertEquals(9, locations.size());
		assertEquals("fourth.md", locations.getLast().toString());

		navigator.navigate(new ExternalUri("fourth.md"));

		// No changes
		assertTrue(navigator.backProperty.get());
		assertFalse(navigator.forwardProperty.get());
		assertEquals(9, locations.size());
		assertEquals("fourth.md", locations.getLast().toString());
	}
}