/*
 * Copyright (c) 2019-2025 by David Gerber - https://zapek.com
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

package io.xeres.app.job;

import io.xeres.app.database.model.connection.ConnectionFakes;
import io.xeres.app.net.peer.bootstrap.PeerI2pClient;
import io.xeres.app.net.peer.bootstrap.PeerTcpClient;
import io.xeres.app.net.peer.bootstrap.PeerTorClient;
import io.xeres.app.net.protocol.PeerAddress;
import io.xeres.app.service.LocationService;
import io.xeres.app.service.PeerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PeerConnectionJobTest
{
	@Mock
	private PeerService peerService;

	@Mock
	private LocationService locationService;

	@Mock
	private PeerTcpClient peerTcpClient;

	@Mock
	private PeerTorClient peerTorClient;

	@Mock
	private PeerI2pClient peerI2pClient;

	@InjectMocks
	private PeerConnectionJob peerConnectionJob;

	@Test
	void IsNotRunning_Success()
	{
		when(peerService.isRunning()).thenReturn(false);

		peerConnectionJob.checkConnections();

		verify(peerService).isRunning();
		verify(locationService, never()).getConnectionsToConnectTo(anyInt());
	}

	@Test
	void ConnectToPeers_TCP_Success()
	{
		when(peerService.isRunning()).thenReturn(true);
		when(locationService.getConnectionsToConnectTo(anyInt())).thenReturn(List.of(ConnectionFakes.createConnection(PeerAddress.Type.IPV4, "1.1.1.1:1234", true)));

		peerConnectionJob.checkConnections();

		verify(peerService).isRunning();
		verify(locationService).getConnectionsToConnectTo(anyInt());
		verify(peerTcpClient).connect(any(PeerAddress.class));
	}

	@Test
	void ConnectToPeers_Tor_Success()
	{
		when(peerService.isRunning()).thenReturn(true);
		when(locationService.getConnectionsToConnectTo(anyInt())).thenReturn(List.of(ConnectionFakes.createConnection(PeerAddress.Type.TOR, "2gzyxa5ihm7nsggfxnu52rck2vv4rvmdlkiu3zzui5du4xyclen53wid.onion:80", true)));

		peerConnectionJob.checkConnections();

		verify(peerService).isRunning();
		verify(locationService).getConnectionsToConnectTo(anyInt());
		verify(peerTorClient).connect(any(PeerAddress.class));
	}

	@Test
	void ConnectToPeers_I2p_Success()
	{
		when(peerService.isRunning()).thenReturn(true);
		when(locationService.getConnectionsToConnectTo(anyInt())).thenReturn(List.of(ConnectionFakes.createConnection(PeerAddress.Type.TOR, "udhdrtrcetjm5sxzskjyr5ztpeszydbh4dpl3pl4utgqqw2v4jna.b32.i2p:80", true)));

		peerConnectionJob.checkConnections();

		verify(peerService).isRunning();
		verify(locationService).getConnectionsToConnectTo(anyInt());
		verify(peerI2pClient).connect(any(PeerAddress.class));
	}
}
