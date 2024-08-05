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

package io.xeres.app.xrs.service.identity;

import io.xeres.app.database.model.gxs.IdentityGroupItemFakes;
import io.xeres.app.net.peer.PeerConnectionFakes;
import io.xeres.app.net.peer.PeerConnectionManager;
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
class IdentityManagerTest
{
	@Mock
	private IdentityRsService identityRsService;

	@Mock
	private PeerConnectionManager peerConnectionManager;

	@InjectMocks
	private IdentityManager identityManager;

	@Test
	void GxsIdManager_AddOneAndRequest_OK()
	{
		var gxsId = IdentityGroupItemFakes.createIdentityGroupItem();
		var peerConnection = PeerConnectionFakes.createPeerConnection();

		when(identityRsService.findByGxsId(gxsId.getGxsId())).thenReturn(Optional.empty());
		when(peerConnectionManager.getPeerByLocationId(peerConnection.getLocation().getId())).thenReturn(peerConnection);

		identityManager.getGxsGroup(peerConnection, gxsId.getGxsId());

		identityManager.requestGxsIds();

		verify(identityRsService).requestGxsGroups(peerConnection, List.of(gxsId.getGxsId()));
	}

	@Test
	@SuppressWarnings("unchecked")
	void GxsIdManager_AddSixAndRequest_OK()
	{
		var gxsId1 = IdentityGroupItemFakes.createIdentityGroupItem();
		var gxsId2 = IdentityGroupItemFakes.createIdentityGroupItem();
		var gxsId3 = IdentityGroupItemFakes.createIdentityGroupItem();
		var gxsId4 = IdentityGroupItemFakes.createIdentityGroupItem();
		var gxsId5 = IdentityGroupItemFakes.createIdentityGroupItem();
		var gxsId6 = IdentityGroupItemFakes.createIdentityGroupItem();
		var peerConnection = PeerConnectionFakes.createPeerConnection();

		when(identityRsService.findByGxsId(any(GxsId.class))).thenReturn(Optional.empty());
		when(peerConnectionManager.getPeerByLocationId(anyLong())).thenReturn(peerConnection);

		identityManager.getGxsGroup(peerConnection, gxsId1.getGxsId());
		identityManager.getGxsGroup(peerConnection, gxsId2.getGxsId());
		identityManager.getGxsGroup(peerConnection, gxsId3.getGxsId());
		identityManager.getGxsGroup(peerConnection, gxsId4.getGxsId());
		identityManager.getGxsGroup(peerConnection, gxsId5.getGxsId());
		identityManager.getGxsGroup(peerConnection, gxsId6.getGxsId());

		identityManager.requestGxsIds();

		ArgumentCaptor<List<GxsId>> ids = ArgumentCaptor.forClass(List.class);
		verify(identityRsService).requestGxsGroups(eq(peerConnection), ids.capture());

		assertEquals(5, ids.getValue().size());

		Set<GxsId> allGxsIds = new HashSet<>();
		allGxsIds.add(gxsId1.getGxsId());
		allGxsIds.add(gxsId2.getGxsId());
		allGxsIds.add(gxsId3.getGxsId());
		allGxsIds.add(gxsId4.getGxsId());
		allGxsIds.add(gxsId5.getGxsId());
		allGxsIds.add(gxsId6.getGxsId());
		ids.getValue().forEach(allGxsIds::remove);
		assertEquals(1, allGxsIds.size());
	}
}
