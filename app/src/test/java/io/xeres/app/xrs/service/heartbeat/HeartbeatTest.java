/*
 * Copyright (c) 2019-2023 by David Gerber - https://zapek.com
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

package io.xeres.app.xrs.service.heartbeat;

import io.xeres.app.database.model.location.Location;
import io.xeres.app.net.peer.PeerConnection;
import io.xeres.app.xrs.service.heartbeat.item.HeartbeatItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class HeartbeatTest
{
	@InjectMocks
	private HeartbeatRsService heartbeatRsService;

	@Test
	@SuppressWarnings("java:S2699")
	void RttService_handleHeartbeat_OK()
	{
		var peerConnection = new PeerConnection(Location.createLocation("foo"), null);

		heartbeatRsService.handleItem(peerConnection, new HeartbeatItem());

		// The service does nothing
	}
}
