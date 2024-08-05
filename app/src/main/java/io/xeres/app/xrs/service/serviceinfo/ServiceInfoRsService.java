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

package io.xeres.app.xrs.service.serviceinfo;

import io.xeres.app.net.peer.PeerConnection;
import io.xeres.app.net.peer.PeerConnectionManager;
import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.service.RsService;
import io.xeres.app.xrs.service.RsServiceInitPriority;
import io.xeres.app.xrs.service.RsServiceRegistry;
import io.xeres.app.xrs.service.RsServiceType;
import io.xeres.app.xrs.service.serviceinfo.item.ServiceInfo;
import io.xeres.app.xrs.service.serviceinfo.item.ServiceListItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static io.xeres.app.xrs.service.RsServiceType.PACKET_SLICING_PROBE;
import static io.xeres.app.xrs.service.RsServiceType.SERVICEINFO;
import static java.util.stream.Collectors.joining;

@Component
public class ServiceInfoRsService extends RsService
{
	private static final Logger log = LoggerFactory.getLogger(ServiceInfoRsService.class);

	private final PeerConnectionManager peerConnectionManager;
	private final RsServiceRegistry rsServiceRegistry;

	public ServiceInfoRsService(RsServiceRegistry rsServiceRegistry, PeerConnectionManager peerConnectionManager)
	{
		super(rsServiceRegistry);
		this.peerConnectionManager = peerConnectionManager;
		this.rsServiceRegistry = rsServiceRegistry;
	}

	public void init(PeerConnection peerConnection)
	{
		sendFirstServiceList(peerConnection); //XXX: if sending and receiving at the same time (5 seconds makes it happen), then it can resend back. solution? put a timer before sending back?
	}

	@Override
	public RsServiceType getServiceType()
	{
		return SERVICEINFO;
	}

	@Override
	public void handleItem(PeerConnection sender, Item item)
	{
		if (item instanceof ServiceListItem serviceListItem)
		{
			// RS requests services twice upon first connection (bug?)
			if (!sender.canSendServices())
			{
				return;
			}

			var services = new PriorityQueue<RsService>();

			serviceListItem.getServices().forEach((integer, serviceInfo) ->
			{
				var rsService = rsServiceRegistry.getServiceFromType(serviceInfo.getType());
				if (rsService != null)
				{
					sender.addService(rsService);
					services.add(rsService);
				}
			});
			if (log.isDebugEnabled())
			{
				log.debug("Enabling services {} to peer {}", services.stream().map(rsService -> rsService.getServiceType().name()).collect(joining(", ")), sender);
			}
			sendFirstServiceList(sender);

			initializeServices(sender, services);
		}
	}

	private void sendFirstServiceList(PeerConnection peerConnection)
	{
		var services = new HashMap<Integer, ServiceInfo>();

		var allServices = rsServiceRegistry.getServices();
		allServices.stream()
				.filter(Predicate.not(rsService -> rsService.getServiceType() == PACKET_SLICING_PROBE)) // we hide this as it's not strictly a service in RS' terms
				.forEach(rsService ->
				{
					var serviceType = rsService.getServiceType();
					var type = 2 << 24 | rsService.getServiceType().getType() << 8;
					services.put(type, new ServiceInfo(serviceType.getName(), type, rsService.getServiceType().getVersionMajor(), rsService.getServiceType().getVersionMinor()));
				});

		peerConnectionManager.writeItem(peerConnection, new ServiceListItem(services), this);
	}

	private static void initializeServices(PeerConnection peerConnection, PriorityQueue<RsService> services)
	{
		RsService rsService;

		while ((rsService = services.poll()) != null)
		{
			if (rsService.getInitPriority() != RsServiceInitPriority.OFF)
			{
				var finalRsService = rsService;
				peerConnection.schedule(() -> finalRsService.initialize(peerConnection),
						ThreadLocalRandom.current().nextInt(rsService.getInitPriority().getMinTime(), rsService.getInitPriority().getMaxTime() + 1),
						TimeUnit.SECONDS);
			}
		}
	}
}
