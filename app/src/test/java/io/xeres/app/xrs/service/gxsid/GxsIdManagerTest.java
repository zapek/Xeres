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

package io.xeres.app.xrs.service.gxsid;

import io.xeres.app.database.model.gxs.GxsIdGroupItemFakes;
import io.xeres.app.net.peer.PeerConnectionFakes;
import io.xeres.app.net.peer.PeerConnectionManager;
import io.xeres.app.service.IdentityService;
import io.xeres.common.id.GxsId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class GxsIdManagerTest
{
	@Mock
	private IdentityService identityService;

	@Mock
	private GxsIdService gxsIdService;

	@Mock
	private PeerConnectionManager peerConnectionManager;

	@InjectMocks
	private GxsIdManager gxsIdManager;

	@Test
	void GxsIdManager_AddOneAndRequest_OK()
	{
		var GXSID = GxsIdGroupItemFakes.createGxsIdGroupItem();
		var PEER_CONNECTION = PeerConnectionFakes.createPeerConnection();

		when(identityService.getGxsIdentity(GXSID.getGxsId())).thenReturn(Optional.empty());
		when(peerConnectionManager.getPeerByLocationId(PEER_CONNECTION.getLocation().getId())).thenReturn(PEER_CONNECTION);

		gxsIdManager.getGxsGroup(PEER_CONNECTION, GXSID.getGxsId());

		gxsIdManager.requestGxsIds();

		verify(gxsIdService).requestGxsGroups(eq(PEER_CONNECTION), eq(List.of(GXSID.getGxsId())));
	}

	@Test
	@SuppressWarnings("unchecked")
	void GxsIdManager_AddSixAndRequest_OK()
	{
		var GXSID1 = GxsIdGroupItemFakes.createGxsIdGroupItem();
		var GXSID2 = GxsIdGroupItemFakes.createGxsIdGroupItem();
		var GXSID3 = GxsIdGroupItemFakes.createGxsIdGroupItem();
		var GXSID4 = GxsIdGroupItemFakes.createGxsIdGroupItem();
		var GXSID5 = GxsIdGroupItemFakes.createGxsIdGroupItem();
		var GXSID6 = GxsIdGroupItemFakes.createGxsIdGroupItem();
		var PEER_CONNECTION = PeerConnectionFakes.createPeerConnection();

		when(identityService.getGxsIdentity(any(GxsId.class))).thenReturn(Optional.empty());
		when(peerConnectionManager.getPeerByLocationId(anyLong())).thenReturn(PEER_CONNECTION);

		gxsIdManager.getGxsGroup(PEER_CONNECTION, GXSID1.getGxsId());
		gxsIdManager.getGxsGroup(PEER_CONNECTION, GXSID2.getGxsId());
		gxsIdManager.getGxsGroup(PEER_CONNECTION, GXSID3.getGxsId());
		gxsIdManager.getGxsGroup(PEER_CONNECTION, GXSID4.getGxsId());
		gxsIdManager.getGxsGroup(PEER_CONNECTION, GXSID5.getGxsId());
		gxsIdManager.getGxsGroup(PEER_CONNECTION, GXSID6.getGxsId());

		gxsIdManager.requestGxsIds();

		ArgumentCaptor<List<GxsId>> ids = ArgumentCaptor.forClass(List.class);
		verify(gxsIdService).requestGxsGroups(eq(PEER_CONNECTION), ids.capture());

		assertEquals(5, ids.getValue().size());

		Set<GxsId> allGxsIds = new HashSet<>();
		allGxsIds.add(GXSID1.getGxsId());
		allGxsIds.add(GXSID2.getGxsId());
		allGxsIds.add(GXSID3.getGxsId());
		allGxsIds.add(GXSID4.getGxsId());
		allGxsIds.add(GXSID5.getGxsId());
		allGxsIds.add(GXSID6.getGxsId());
		ids.getValue().forEach(allGxsIds::remove);
		assertEquals(1, allGxsIds.size());
	}
}
