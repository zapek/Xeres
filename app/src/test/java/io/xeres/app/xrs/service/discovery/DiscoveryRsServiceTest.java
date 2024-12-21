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

package io.xeres.app.xrs.service.discovery;

import io.xeres.app.database.model.location.Location;
import io.xeres.app.database.model.location.LocationFakes;
import io.xeres.app.database.model.profile.ProfileFakes;
import io.xeres.app.net.peer.PeerConnection;
import io.xeres.app.net.peer.PeerConnectionManager;
import io.xeres.app.service.LocationService;
import io.xeres.app.service.ProfileService;
import io.xeres.app.service.notification.status.StatusNotificationService;
import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.service.RsService;
import io.xeres.app.xrs.service.discovery.item.DiscoveryContactItem;
import io.xeres.app.xrs.service.discovery.item.DiscoveryPgpListItem;
import io.xeres.common.id.LocationIdentifier;
import io.xeres.common.protocol.NetMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class DiscoveryRsServiceTest
{
	@Mock
	private PeerConnectionManager peerConnectionManager;

	@Mock
	private ProfileService profileService;

	@Mock
	private LocationService locationService;

	@SuppressWarnings("unused")
	@Mock
	private StatusNotificationService statusNotificationService;

	@InjectMocks
	private DiscoveryRsService discoveryRsService;

	/**
	 * This is a case that is handled by RS but that I think is never actually sent.
	 * We ignore it, just in case.
	 */
	@Test
	void HandleDiscoveryContactItem_NewLocation_FriendOfFriend_Known_Ignore()
	{
		var peerConnection = new PeerConnection(LocationFakes.createLocation(), null);

		when(locationService.findLocationByLocationIdentifier(any(LocationIdentifier.class))).thenReturn(Optional.empty());
		when(profileService.findProfileByPgpIdentifier(anyLong())).thenReturn(Optional.empty());

		discoveryRsService.handleItem(peerConnection, createDiscoveryContact(LocationFakes.createLocation()));

		verify(peerConnectionManager, never()).writeItem(eq(peerConnection), any(Item.class), any(RsService.class));
	}

	/**
	 * This is a case that shouldn't happen either.
	 */
	@Test
	void HandleDiscoveryContactItem_NewLocation_FriendOfFriend_Unknown_Ignore()
	{
		var peerConnection = new PeerConnection(LocationFakes.createLocation(), null);

		when(locationService.findLocationByLocationIdentifier(any(LocationIdentifier.class))).thenReturn(Optional.empty());
		when(profileService.findProfileByPgpIdentifier(anyLong())).thenReturn(Optional.of(ProfileFakes.createProfile()));

		discoveryRsService.handleItem(peerConnection, createDiscoveryContact(LocationFakes.createLocation()));

		verify(peerConnectionManager, never()).writeItem(eq(peerConnection), any(Item.class), any(RsService.class));
	}

	/**
	 * The peer sends the new location of a common friend. We keep that new location.
	 */
	@Test
	void HandleDiscoveryContactItem_NewLocation_Friend_Success()
	{
		var peerLocation = LocationFakes.createLocation();
		var peerConnection = new PeerConnection(peerLocation, null);
		var profile = ProfileFakes.createProfile();
		profile.setAccepted(true);
		var newLocation = LocationFakes.createLocation("foo", profile);

		when(locationService.findLocationByLocationIdentifier(peerLocation.getLocationIdentifier())).thenReturn(Optional.empty());
		when(profileService.findProfileByPgpIdentifier(profile.getPgpIdentifier())).thenReturn(Optional.of(profile));

		discoveryRsService.handleItem(peerConnection, createDiscoveryContact(newLocation));

		verify(peerConnectionManager, never()).writeItem(eq(peerConnection), any(Item.class), any(RsService.class));
		verify(locationService).update(eq(newLocation), anyString(), any(NetMode.class), anyString(), any(), anyList());
	}

	/**
	 * The peer sends an updated location of a common friend. We update
	 * the location.
	 */
	@Test
	void HandleDiscoveryContactItem_UpdateLocation_Friend_Success()
	{
		var peerLocation = LocationFakes.createLocation();
		var peerConnection = new PeerConnection(peerLocation, null);
		var profile = ProfileFakes.createProfile();
		profile.setAccepted(true);
		var friendLocation = LocationFakes.createLocation("foo", profile);

		when(locationService.findLocationByLocationIdentifier(friendLocation.getLocationIdentifier())).thenReturn(Optional.of(friendLocation));
		when(locationService.findOwnLocation()).thenReturn(Optional.of(LocationFakes.createOwnLocation()));

		discoveryRsService.handleItem(peerConnection, createDiscoveryContact(friendLocation));

		verify(peerConnectionManager, never()).writeItem(eq(peerConnection), any(Item.class), any(RsService.class));
		verify(locationService).update(eq(friendLocation), anyString(), any(NetMode.class), anyString(), any(), anyList());
	}

	/**
	 * The peer sends our own location. We do nothing (could be used to help find out our external
	 * IP address).
	 */
	@Test
	void HandleDiscoveryContactItem_UpdateLocation_Own_Ignore()
	{
		var peerLocation = LocationFakes.createLocation();
		var peerConnection = new PeerConnection(peerLocation, null);
		var profile = ProfileFakes.createProfile();
		profile.setAccepted(true);
		var friendLocation = LocationFakes.createLocation("foo", profile);

		when(locationService.findLocationByLocationIdentifier(friendLocation.getLocationIdentifier())).thenReturn(Optional.of(friendLocation));
		when(locationService.findOwnLocation()).thenReturn(Optional.of(friendLocation));

		discoveryRsService.handleItem(peerConnection, createDiscoveryContact(friendLocation));

		verify(peerConnectionManager, never()).writeItem(eq(peerConnection), any(Item.class), any(RsService.class));
	}

	/**
	 * The peer sends his location. We update its location and send our list
	 * of friends.
	 */
	@Test
	void HandleDiscoveryContactItem_UpdateLocation_Peer_Success()
	{
		var peerLocation = LocationFakes.createLocation();
		var peerConnection = new PeerConnection(peerLocation, null);
		var ownLocation = LocationFakes.createLocation();
		var profile = ProfileFakes.createProfile();
		profile.setAccepted(true);

		when(locationService.findLocationByLocationIdentifier(peerLocation.getLocationIdentifier())).thenReturn(Optional.of(peerLocation));
		when(locationService.findOwnLocation()).thenReturn(Optional.of(ownLocation));
		when(profileService.getAllDiscoverableProfiles()).thenReturn(List.of(profile));

		discoveryRsService.handleItem(peerConnection, createDiscoveryContact(peerLocation));

		verify(locationService).update(eq(peerLocation), anyString(), any(NetMode.class), anyString(), any(), anyList());
		var discoveryPgpListItem = ArgumentCaptor.forClass(DiscoveryPgpListItem.class);
		verify(peerConnectionManager).writeItem(eq(peerConnection), discoveryPgpListItem.capture(), any(RsService.class));

		assertEquals(DiscoveryPgpListItem.Mode.FRIENDS, discoveryPgpListItem.getValue().getMode());
		assertTrue(discoveryPgpListItem.getValue().getPgpIds().contains(profile.getPgpIdentifier()));
	}

	/**
	 * The peer sends his location. We update its location but don't send our list of
	 * friends because we're not discoverable.
	 */
	@Test
	void HandleDiscoveryContactItem_UpdateLocation_Peer_OurLocation_NotDiscoverable_Success()
	{
		var peerLocation = LocationFakes.createLocation();
		var peerConnection = new PeerConnection(peerLocation, null);
		var ownLocation = LocationFakes.createLocation();
		ownLocation.setDiscoverable(false);

		when(locationService.findLocationByLocationIdentifier(peerLocation.getLocationIdentifier())).thenReturn(Optional.of(peerLocation));
		when(locationService.findOwnLocation()).thenReturn(Optional.of(ownLocation));

		discoveryRsService.handleItem(peerConnection, createDiscoveryContact(peerLocation));

		verify(locationService).findLocationByLocationIdentifier(peerLocation.getLocationIdentifier());
		verify(locationService).findOwnLocation();
		verify(locationService).update(eq(peerLocation), anyString(), any(NetMode.class), anyString(), any(), anyList());
		verify(peerConnectionManager, never()).writeItem(eq(peerConnection), any(Item.class), any(RsService.class));
	}

	/**
	 * The peer sends his location. We update its location and since it's a partial profile (added through
	 * ShortInvites) we ask for its PGP key.
	 */
	@Test
	void HandleDiscoveryContactItem_UpdateLocation_Peer_Partial_Success()
	{
		var peerLocation = LocationFakes.createLocation();
		var peerConnection = new PeerConnection(peerLocation, null);
		var peerProfile = ProfileFakes.createProfile();
		peerProfile.setAccepted(true);
		peerProfile.setPgpPublicKeyData(null); // partial profile
		peerLocation.setProfile(peerProfile);

		when(locationService.findLocationByLocationIdentifier(peerLocation.getLocationIdentifier())).thenReturn(Optional.of(peerLocation));

		discoveryRsService.handleItem(peerConnection, createDiscoveryContact(peerLocation));

		verify(locationService).update(eq(peerLocation), anyString(), any(NetMode.class), anyString(), any(), anyList());
		var discoveryPgpListItem = ArgumentCaptor.forClass(DiscoveryPgpListItem.class);
		verify(peerConnectionManager).writeItem(eq(peerConnection), discoveryPgpListItem.capture(), any(RsService.class));

		assertEquals(DiscoveryPgpListItem.Mode.GET_CERT, discoveryPgpListItem.getValue().getMode());
		assertTrue(discoveryPgpListItem.getValue().getPgpIds().contains(peerProfile.getPgpIdentifier()));
	}

	private DiscoveryContactItem createDiscoveryContact(Location location)
	{
		var builder = DiscoveryContactItem.builder();

		builder.setPgpIdentifier(location.getProfile().getPgpIdentifier());
		builder.setLocationIdentifier(location.getLocationIdentifier());
		builder.setLocationName(location.getName());
		builder.setHostname("foobar.com"); // XXX: no hostname support in location yet
		builder.setNetMode(location.getNetMode());
		builder.setVersion(location.getVersion());
		return builder.build();
	}
}
