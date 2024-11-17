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

package io.xeres.app.xrs.service.status;

import io.xeres.app.database.DatabaseSessionManager;
import io.xeres.app.database.model.location.LocationFakes;
import io.xeres.app.net.peer.PeerConnectionManager;
import io.xeres.app.service.LocationService;
import io.xeres.app.service.notification.availability.AvailabilityNotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;

import static io.xeres.common.location.Availability.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class StatusRsServiceTest
{

	@Mock
	private PeerConnectionManager peerConnectionManager;

	@Mock
	private LocationService locationService;

	@Mock
	private AvailabilityNotificationService availabilityNotificationService;

	@Mock
	private DatabaseSessionManager databaseSessionManager;

	@InjectMocks
	private StatusRsService statusRsService;

	@Test
	void Change_Availability_All_Success()
	{
		var ownLocation = LocationFakes.createOwnLocation();

		when(locationService.findOwnLocation()).thenReturn(Optional.of((ownLocation)));

		statusRsService.changeAvailability(BUSY);
		statusRsService.changeAvailability(AWAY);
		statusRsService.changeAvailability(AVAILABLE);

		verify(availabilityNotificationService).changeAvailability(ownLocation, BUSY);
		verify(availabilityNotificationService).changeAvailability(ownLocation, AWAY);
		verify(availabilityNotificationService).changeAvailability(ownLocation, AVAILABLE);
	}

	@Test
	void Change_Availability_Away_And_Back_Success()
	{
		var ownLocation = LocationFakes.createOwnLocation();

		when(locationService.findOwnLocation()).thenReturn(Optional.of((ownLocation)));

		statusRsService.changeAvailability(AWAY);
		statusRsService.changeAvailability(AVAILABLE);
		statusRsService.changeAvailability(AWAY);

		verify(availabilityNotificationService).changeAvailability(ownLocation, AVAILABLE);
		verify(availabilityNotificationService, times(2)).changeAvailability(ownLocation, AWAY);
	}

	@Test
	void Manual_Prevents_Automatic()
	{
		var ownLocation = LocationFakes.createOwnLocation();

		when(locationService.findOwnLocation()).thenReturn(Optional.of((ownLocation)));

		statusRsService.changeAvailabilityAutomatically(AWAY);
		statusRsService.changeAvailabilityAutomatically(AVAILABLE);

		// Lock
		statusRsService.changeAvailability(BUSY);

		statusRsService.changeAvailabilityAutomatically(AWAY);

		// Unlock
		statusRsService.changeAvailability(AVAILABLE);

		statusRsService.changeAvailabilityAutomatically(AWAY);

		verify(availabilityNotificationService, times(2)).changeAvailability(ownLocation, AVAILABLE);
		verify(availabilityNotificationService, times(2)).changeAvailability(ownLocation, AWAY);
		verify(availabilityNotificationService).changeAvailability(ownLocation, BUSY);
	}
}