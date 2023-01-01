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
class IdentityManagerTest
{
	@Mock
	private IdentityService identityService;

	@Mock
	private IdentityRsService identityRsService;

	@Mock
	private PeerConnectionManager peerConnectionManager;

	@InjectMocks
	private IdentityManager identityManager;

	@Test
	void GxsIdManager_AddOneAndRequest_OK()
	{
		var GXSID = IdentityGroupItemFakes.createIdentityGroupItem();
		var PEER_CONNECTION = PeerConnectionFakes.createPeerConnection();

		when(identityService.findByGxsId(GXSID.getGxsId())).thenReturn(Optional.empty());
		when(peerConnectionManager.getPeerByLocationId(PEER_CONNECTION.getLocation().getId())).thenReturn(PEER_CONNECTION);

		identityManager.getGxsGroup(PEER_CONNECTION, GXSID.getGxsId());

		identityManager.requestGxsIds();

		verify(identityRsService).requestGxsGroups(PEER_CONNECTION, List.of(GXSID.getGxsId()));
	}

	@Test
	@SuppressWarnings("unchecked")
	void GxsIdManager_AddSixAndRequest_OK()
	{
		var GXSID1 = IdentityGroupItemFakes.createIdentityGroupItem();
		var GXSID2 = IdentityGroupItemFakes.createIdentityGroupItem();
		var GXSID3 = IdentityGroupItemFakes.createIdentityGroupItem();
		var GXSID4 = IdentityGroupItemFakes.createIdentityGroupItem();
		var GXSID5 = IdentityGroupItemFakes.createIdentityGroupItem();
		var GXSID6 = IdentityGroupItemFakes.createIdentityGroupItem();
		var PEER_CONNECTION = PeerConnectionFakes.createPeerConnection();

		when(identityService.findByGxsId(any(GxsId.class))).thenReturn(Optional.empty());
		when(peerConnectionManager.getPeerByLocationId(anyLong())).thenReturn(PEER_CONNECTION);

		identityManager.getGxsGroup(PEER_CONNECTION, GXSID1.getGxsId());
		identityManager.getGxsGroup(PEER_CONNECTION, GXSID2.getGxsId());
		identityManager.getGxsGroup(PEER_CONNECTION, GXSID3.getGxsId());
		identityManager.getGxsGroup(PEER_CONNECTION, GXSID4.getGxsId());
		identityManager.getGxsGroup(PEER_CONNECTION, GXSID5.getGxsId());
		identityManager.getGxsGroup(PEER_CONNECTION, GXSID6.getGxsId());

		identityManager.requestGxsIds();

		ArgumentCaptor<List<GxsId>> ids = ArgumentCaptor.forClass(List.class);
		verify(identityRsService).requestGxsGroups(eq(PEER_CONNECTION), ids.capture());

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
