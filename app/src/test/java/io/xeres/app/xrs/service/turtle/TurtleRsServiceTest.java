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

package io.xeres.app.xrs.service.turtle;

import io.xeres.app.database.DatabaseSessionManager;
import io.xeres.app.database.model.location.LocationFakes;
import io.xeres.app.service.LocationService;
import io.xeres.app.xrs.service.turtle.item.TurtleTunnelRequestItem;
import io.xeres.common.id.Id;
import io.xeres.common.id.LocationId;
import io.xeres.common.id.Sha1Sum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class TurtleRsServiceTest
{
	@Mock
	private LocationService locationService;

	@Mock
	private DatabaseSessionManager databaseSessionManager;

	@InjectMocks
	private TurtleRsService turtleRsService;

	@Test
	void GeneratePersonalFilePrint_Success()
	{
		// Values have been taken directly from Retroshare to make sure there's no signed/unsigned bugs
		var ownLocation = LocationFakes.createLocation("Test", null, LocationId.fromString("d3b9c7ceb75c7c68b5e3c6446259c8e7"));

		when(locationService.findOwnLocation()).thenReturn(Optional.of(ownLocation));
		turtleRsService.initialize();

		var item = mock(TurtleTunnelRequestItem.class);
		when(item.getHash()).thenReturn(new Sha1Sum(Id.toBytes("ac39b8f761465b1460948973e8fe754f4e101700")));
		var result = turtleRsService.generatePersonalFilePrint(item.getHash(), 1_833_303_450, true);

		assertEquals(3_280_770_886L, Integer.toUnsignedLong(result));
	}
}