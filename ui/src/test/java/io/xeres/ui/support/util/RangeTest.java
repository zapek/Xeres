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

package io.xeres.ui.support.util;

import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class RangeTest
{
	@Test
	void testIndexConstructor_hasRangeAndAccessors()
	{
		// Step 2: index-based constructor
		Range r = new Range(5, 10); // (plan step 2)
		assertEquals(5, r.start(), "start should match given start");
		assertEquals(10, r.end(), "end should match given end");
		assertTrue(r.hasRange(), "end > start -> hasRange should be true");

		Range empty = new Range(7, 7);
		assertFalse(empty.hasRange(), "start == end -> hasRange should be false");
	}

	@Test
	void testMatcherConstructor_wholeMatch()
	{
		// Step 3a: whole-match (no capture groups)
		Pattern p = Pattern.compile("hello");
		Matcher m = p.matcher("hello world");
		assertTrue(m.find(), "pattern should find a match");

		Range r = new Range(m); // (plan step 3a)
		assertEquals(m.start(), r.start(), "start should equal match start");
		assertEquals(m.end(), r.end(), "end should equal match end");
		// For whole-match constructor, Range.group remains default (0) and groupName stays null
		assertEquals(0, r.group(), "group should be 0 for whole-match constructor");
		assertNull(r.groupName(), "groupName should be null for whole-match constructor");
	}

	@Test
	void testMatcherConstructor_unnamedGroup()
	{
		// Step 3b: unnamed capture group
		Pattern p = Pattern.compile("(foo)bar");
		Matcher m = p.matcher("foobar");
		assertTrue(m.find(), "pattern should find a match");

		Range r = new Range(m); // (plan step 3b)
		// The constructor will pick the first matched capturing group (group 1)
		assertEquals(m.start(1), r.start(), "start should match capture group start");
		assertEquals(m.end(1), r.end(), "end should match capture group end");
		assertEquals(1, r.group(), "group should be 1 for the first capturing group");
		// For unnamed group the code sets groupName to empty string
		assertEquals("", r.groupName(), "groupName should be empty string for unnamed capture group");
	}

	@Test
	void testMatcherConstructor_namedGroup()
	{
		// Step 3c: named capture group
		Pattern p = Pattern.compile("(?<name>bar)baz");
		Matcher m = p.matcher("barbaz");
		assertTrue(m.find(), "pattern should find a match with named group");

		Range r = new Range(m); // (plan step 3c)
		// Named group 'name' is group 1 here
		assertEquals(m.start(1), r.start(), "start should match named group start");
		assertEquals(m.end(1), r.end(), "end should match named group end");
		assertEquals(1, r.group(), "group should be the numeric index of the named group");
		assertEquals("name", r.groupName(), "groupName should be the name of the named capture group");
	}

	@Test
	void testOuterRange_otherAfter()
	{
		// Step 4: outerRange when other is after us
		Range a = new Range(0, 3);
		Range b = new Range(5, 8);
		Range gap = a.outerRange(b); // (plan step 4)
		assertEquals(3, gap.start(), "gap start should be end of first range");
		assertEquals(5, gap.end(), "gap end should be start of second range");
		assertTrue(gap.hasRange(), "non-empty gap should have range");
	}

	@Test
	void testOuterRange_otherBefore()
	{
		// Step 4: outerRange when other is before us
		Range a = new Range(10, 15);
		Range b = new Range(2, 7);
		Range gap = a.outerRange(b); // (plan step 4)
		assertEquals(7, gap.start(), "gap start should be end of the other range");
		assertEquals(10, gap.end(), "gap end should be start of this range");
		assertTrue(gap.hasRange(), "non-overlapping ranges should yield a non-empty gap");
	}
}