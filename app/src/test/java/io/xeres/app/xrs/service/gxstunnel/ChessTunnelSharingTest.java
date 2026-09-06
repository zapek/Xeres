/*
 * Copyright (c) 2019-2026 by David Gerber - https://zapek.com
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

package io.xeres.app.xrs.service.gxstunnel;

import io.xeres.app.database.DatabaseSessionManager;
import io.xeres.app.service.IdentityService;
import io.xeres.app.xrs.service.RsServiceRegistry;
import io.xeres.app.xrs.service.turtle.TurtleRouter;
import io.xeres.common.id.GxsId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChessTunnelSharingTest
{
	@Test
	void chessSharesChatTunnelAndReleasePreservesChat()
	{
		var router = mock(TurtleRouter.class);
		var service = new GxsTunnelRsService(mock(RsServiceRegistry.class), mock(DatabaseSessionManager.class), mock(IdentityService.class));
		service.initializeTurtle(router);
		var own = GxsId.fromString("11".repeat(16));
		var peer = GxsId.fromString("22".repeat(16));
		var chatTunnel = service.requestSecuredTunnel(own, peer, 0x12);
		assertNotNull(chatTunnel);
		assertEquals(chatTunnel, service.requestSecuredTunnel(own, peer, 0xC4E5));
		assertNull(service.requestSecuredTunnel(own, peer, 0x12));
		service.releaseTunnelService(chatTunnel, 0xC4E5);
		assertEquals(peer, service.getGxsFromTunnel(chatTunnel));
		assertEquals(chatTunnel, service.requestSecuredTunnel(own, peer, 0xC4E5));
		verify(router, times(1)).startMonitoringTunnels(any(), eq(service), eq(false));
		verify(router, never()).stopMonitoringTunnels(any());
	}

	@Test
	void releasingOnlyServiceCancelsUnestablishedTunnel()
	{
		var router = mock(TurtleRouter.class);
		var service = new GxsTunnelRsService(mock(RsServiceRegistry.class), mock(DatabaseSessionManager.class), mock(IdentityService.class));
		service.initializeTurtle(router);
		var own = GxsId.fromString("11".repeat(16));
		var peer = GxsId.fromString("22".repeat(16));
		var tunnel = service.requestSecuredTunnel(own, peer, 0xC4E5);
		service.releaseTunnelService(tunnel, 0xC4E5);
		assertNull(service.getGxsFromTunnel(tunnel));
		verify(router).stopMonitoringTunnels(any());
	}
}
