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

package io.xeres.app.xrs.service.status;

import io.xeres.app.net.peer.PeerConnection;
import io.xeres.app.net.peer.PeerConnectionManager;
import io.xeres.app.service.LocationService;
import io.xeres.app.service.notification.availability.AvailabilityNotificationService;
import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.service.RsService;
import io.xeres.app.xrs.service.RsServiceInitPriority;
import io.xeres.app.xrs.service.RsServiceRegistry;
import io.xeres.app.xrs.service.RsServiceType;
import io.xeres.app.xrs.service.status.item.StatusItem;
import io.xeres.common.location.Availability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

import static io.xeres.app.xrs.service.RsServiceType.STATUS;
import static io.xeres.common.message.MessageType.CHAT_AVAILABILITY;
import static io.xeres.common.rest.PathConfig.CHAT_PATH;

@Component
public class StatusRsService extends RsService
{
	private static final Logger log = LoggerFactory.getLogger(StatusRsService.class);

	private Availability availability = Availability.AVAILABLE;

	private final PeerConnectionManager peerConnectionManager;
	private final LocationService locationService;
	private final AvailabilityNotificationService availabilityNotificationService;

	private boolean locked;

	public StatusRsService(RsServiceRegistry rsServiceRegistry, PeerConnectionManager peerConnectionManager, LocationService locationService, AvailabilityNotificationService availabilityNotificationService)
	{
		super(rsServiceRegistry);
		this.peerConnectionManager = peerConnectionManager;
		this.locationService = locationService;
		this.availabilityNotificationService = availabilityNotificationService;
	}

	@Override
	public RsServiceType getServiceType()
	{
		return STATUS;
	}

	@Override
	public RsServiceInitPriority getInitPriority()
	{
		return RsServiceInitPriority.NORMAL;
	}

	@Override
	public void initialize(PeerConnection peerConnection)
	{
		peerConnection.schedule(
				() -> peerConnectionManager.writeItem(peerConnection, new StatusItem(ChatStatus.ONLINE), this),
				0,
				TimeUnit.SECONDS);
	}

	@Transactional
	@Override
	public void handleItem(PeerConnection sender, Item item)
	{
		if (item instanceof StatusItem statusItem)
		{
			log.debug("Got status {} from peer {}", statusItem.getStatus(), sender);
			var newStatus = toAvailability(statusItem.getStatus());
			locationService.setAvailability(sender.getLocation(), newStatus);
			availabilityNotificationService.changeAvailability(sender.getLocation(), newStatus);
			peerConnectionManager.sendToClientSubscriptions(CHAT_PATH, CHAT_AVAILABILITY, sender.getLocation().getLocationId(), newStatus);
		}
	}

	private Availability toAvailability(ChatStatus status)
	{
		return switch (status)
		{
			case ONLINE -> Availability.AVAILABLE;
			case AWAY, INACTIVE, OFFLINE -> Availability.AWAY;
			case BUSY -> Availability.BUSY;
		};
	}

	private ChatStatus toChatStatus(Availability availability)
	{
		return switch (availability)
		{
			case AVAILABLE -> ChatStatus.ONLINE;
			case AWAY -> ChatStatus.AWAY;
			case BUSY -> ChatStatus.BUSY;
			case OFFLINE -> ChatStatus.OFFLINE;
		};
	}

	public void changeAvailability(Availability availability)
	{
		locked = false;
		changeAvailabilityAutomatically(availability);
		locked = availability != Availability.AVAILABLE;
	}

	public void changeAvailabilityAutomatically(Availability availability)
	{
		if (!locked && availability != this.availability)
		{
			var ownLocation = locationService.findOwnLocation().orElseThrow();
			this.availability = availability;
			locationService.setAvailability(ownLocation, availability);
			availabilityNotificationService.changeAvailability(ownLocation, availability);
			peerConnectionManager.doForAllPeers(peerConnection -> peerConnectionManager.writeItem(peerConnection, new StatusItem(toChatStatus(availability)), this), this);
		}
	}
}
