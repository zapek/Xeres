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

package io.xeres.app.xrs.service.rtt;

import io.xeres.app.database.model.location.Location;
import io.xeres.app.net.peer.PeerConnection;
import io.xeres.app.net.peer.PeerConnectionManager;
import io.xeres.app.xrs.service.RsService;
import io.xeres.app.xrs.service.rtt.item.RttPingItem;
import io.xeres.app.xrs.service.rtt.item.RttPongItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(SpringExtension.class)
class RttRsServiceTest
{
	@Mock
	private PeerConnectionManager peerConnectionManager;

	@InjectMocks
	private RttRsService rttRsService;

	@Test
	void HandlePing_Success()
	{
		var sequence = 1;
		var timestamp = 2L;

		var peerConnection = new PeerConnection(Location.createLocation("foo"), null);

		rttRsService.handleItem(peerConnection, new RttPingItem(sequence, timestamp));

		var rttPongItem = ArgumentCaptor.forClass(RttPongItem.class);
		verify(peerConnectionManager).writeItem(eq(peerConnection), rttPongItem.capture(), any(RsService.class));

		assertEquals(timestamp, rttPongItem.getValue().getPingTimestamp());
		assertNotEquals(0, rttPongItem.getValue().getPongTimestamp());
	}
}
