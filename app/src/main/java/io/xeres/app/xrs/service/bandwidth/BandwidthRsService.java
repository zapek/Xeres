/*
 * Copyright (c) 2025 by David Gerber - https://zapek.com
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

package io.xeres.app.xrs.service.bandwidth;

import io.xeres.app.net.peer.PeerConnection;
import io.xeres.app.net.peer.PeerConnectionManager;
import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.service.RsService;
import io.xeres.app.xrs.service.RsServiceInitPriority;
import io.xeres.app.xrs.service.RsServiceRegistry;
import io.xeres.app.xrs.service.RsServiceType;
import io.xeres.app.xrs.service.bandwidth.item.BandwidthAllowedItem;
import io.xeres.common.rest.statistics.DataCounterPeer;
import io.xeres.common.rest.statistics.DataCounterStatisticsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static io.xeres.app.xrs.service.RsServiceType.BANDWIDTH_CONTROL;

@Component
public class BandwidthRsService extends RsService
{
	private static final Logger log = LoggerFactory.getLogger(BandwidthRsService.class);

	private final PeerConnectionManager peerConnectionManager;
	private long currentBandwidth;

	BandwidthRsService(RsServiceRegistry rsServiceRegistry, PeerConnectionManager peerConnectionManager)
	{
		super(rsServiceRegistry);
		this.peerConnectionManager = peerConnectionManager;
	}

	@Override
	public RsServiceType getServiceType()
	{
		return BANDWIDTH_CONTROL;
	}

	@Override
	public RsServiceInitPriority getInitPriority()
	{
		return RsServiceInitPriority.HIGH;
	}

	@Override
	public void initialize()
	{
		Thread.ofVirtual().name("Bandwidth Finder").start(() -> currentBandwidth = BandwidthUtils.findBandwidth());
	}

	@Override
	public void initialize(PeerConnection peerConnection)
	{
		peerConnection.schedule(
				() -> sendBandwidthCapabilities(peerConnection)
				, 10,
				TimeUnit.SECONDS
		);
	}

	private void sendBandwidthCapabilities(PeerConnection peerConnection)
	{
		if (currentBandwidth != 0L)
		{
			log.debug("Sending Bandwidth of {} bit/s to peer {}", currentBandwidth, peerConnection);
			peerConnectionManager.writeItem(peerConnection, new BandwidthAllowedItem((long) (currentBandwidth * 0.75 / 1000)), this); // RS wants bytes/s, and it defaults to 75% of the bandwidth
			// TODO: the computation should be improved
		}
	}

	@Override
	public void handleItem(PeerConnection sender, Item item)
	{
		if (item instanceof BandwidthAllowedItem bandwidthAllowedItem)
		{
			log.debug("Allowed bandwidth for peer {}: {} bytes/s", sender, bandwidthAllowedItem.getAllowedBandwidth());
			// XXX: store it on the peer
		}
	}

	@Transactional(readOnly = true)
	public DataCounterStatisticsResponse getDataCounterStatistics()
	{
		List<DataCounterPeer> peers = new ArrayList<>();
		peerConnectionManager.doForAllPeers(peerConnection -> peers.add(new DataCounterPeer(peerConnection.getLocation().getId(),
				peerConnection.getLocation().getProfile().getName() + "@" + peerConnection.getLocation().getSafeName(),
				peerConnection.getSentCounter(),
				peerConnection.getReceivedCounter())), null);
		return new DataCounterStatisticsResponse(peers);
	}
}
