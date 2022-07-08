/*
 * Copyright (c) 2019-2021 by David Gerber - https://zapek.com
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

package io.xeres.app.net.bdisc;

import io.xeres.app.database.DatabaseSessionManager;
import io.xeres.app.database.model.location.LocationFakes;
import io.xeres.app.service.LocationService;
import io.xeres.common.id.LocationId;
import io.xeres.common.protocol.ip.IP;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Duration;
import java.util.Optional;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class BroadcastDiscoveryServiceTest
{
	@Mock
	private LocationService locationService;

	@Mock
	private DatabaseSessionManager databaseSessionManager;

	@InjectMocks
	private BroadcastDiscoveryService broadcastDiscoveryService;

	@Test
	void BroadcastDiscoveryService_StartStop_OK()
	{
		var ownLocation = LocationFakes.createOwnLocation();
		when(locationService.findOwnLocation()).thenReturn(Optional.of(ownLocation));
		when(locationService.findLocationByLocationId(any(LocationId.class))).thenReturn(Optional.empty());

		broadcastDiscoveryService.start(IP.getLocalIpAddress(), 36406); // nothing should reply in there, hopefully. We can't use localhost because linux has no broadcast in it
		await().atMost(Duration.ofSeconds(10)).until(() -> broadcastDiscoveryService.isRunning());

		broadcastDiscoveryService.stop();
		broadcastDiscoveryService.waitForTermination();
		assertFalse(broadcastDiscoveryService.isRunning());
		verify(locationService).findOwnLocation();
	}
}
