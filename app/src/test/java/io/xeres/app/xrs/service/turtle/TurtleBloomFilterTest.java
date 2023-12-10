/*
 * Copyright (c) 2023 by David Gerber - https://zapek.com
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

package io.xeres.app.xrs.service.turtle;

import io.xeres.testutils.Sha1SumFakes;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TurtleBloomFilterTest
{
	@Test
	void TurtleBloomFilter_OK()
	{
		var filter = new TurtleBloomFilter(null, 10_000, 0.01d);

		var s1 = Sha1SumFakes.createSha1Sum();
		var s2 = Sha1SumFakes.createSha1Sum();

		filter.add(s1);
		filter.add(s2);

		assertTrue(filter.mightContain(s1));
		assertTrue(filter.mightContain(s2));

		filter.clear();

		assertFalse(filter.mightContain(s1));
		assertFalse(filter.mightContain(s2));
	}

	@Test
	void TurtleBloomFilter_Multiple_OK()
	{
		var filter = new TurtleBloomFilter(null, 10_000, 0.01d);

		var s1 = Sha1SumFakes.createSha1Sum();
		var s2 = Sha1SumFakes.createSha1Sum();
		var s3 = Sha1SumFakes.createSha1Sum();
		var s4 = Sha1SumFakes.createSha1Sum();
		var s5 = Sha1SumFakes.createSha1Sum();
		var s6 = Sha1SumFakes.createSha1Sum();


		var in = List.of(s1, s2, s3);
		var out = List.of(s4, s5, s6);

		filter.addAll(in);

		assertTrue(filter.mightContainAll(in));
		assertFalse(filter.mightContainAll(out));
	}
}
