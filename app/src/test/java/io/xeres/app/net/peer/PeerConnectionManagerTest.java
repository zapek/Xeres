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

package io.xeres.app.net.peer;

import io.xeres.app.database.model.location.LocationFakes;
import io.xeres.app.service.notification.availability.AvailabilityNotificationService;
import io.xeres.app.service.notification.status.StatusNotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(SpringExtension.class)
class PeerConnectionManagerTest
{
	@Mock
	private StatusNotificationService statusNotificationService;

	@Mock
	private AvailabilityNotificationService availabilityNotificationService;

	@Mock
	ApplicationEventPublisher publisher;

	@InjectMocks
	private PeerConnectionManager peerConnectionManager;

	@Test
	void addAndRemovePeers()
	{
		var location = LocationFakes.createLocation();

		var peerConnection = peerConnectionManager.addPeer(location, new ChannelHandlerContextFake());
		assertNotNull(peerConnection);
		assertEquals(1, peerConnectionManager.getNumberOfPeers());
		assertEquals(peerConnection, peerConnectionManager.getPeerByLocation(location.getId()));
		assertEquals(peerConnection, peerConnectionManager.getRandomPeer());
		peerConnectionManager.removePeer(location);
		assertEquals(0, peerConnectionManager.getNumberOfPeers());
	}
}