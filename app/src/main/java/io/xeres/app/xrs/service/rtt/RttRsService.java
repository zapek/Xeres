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

package io.xeres.app.xrs.service.rtt;

import io.xeres.app.net.peer.PeerConnection;
import io.xeres.app.net.peer.PeerConnectionManager;
import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.service.RsService;
import io.xeres.app.xrs.service.RsServiceInitPriority;
import io.xeres.app.xrs.service.RsServiceType;
import io.xeres.app.xrs.service.rtt.item.RttPingItem;
import io.xeres.app.xrs.service.rtt.item.RttPongItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static io.xeres.app.xrs.service.RsServiceType.RTT;

@Component
public class RttRsService extends RsService
{
	private static final Logger log = LoggerFactory.getLogger(RttRsService.class);

	private final PeerConnectionManager peerConnectionManager;

	private static final int KEY_COUNTER = 1;

	public RttRsService(Environment environment, PeerConnectionManager peerConnectionManager)
	{
		super(environment);
		this.peerConnectionManager = peerConnectionManager;
	}

	@Override
	public RsServiceType getServiceType()
	{
		return RTT;
	}

	public Map<Class<? extends Item>, Integer> getSupportedItems()
	{
		return Map.of(
				RttPingItem.class, 1,
				RttPongItem.class, 2
		);
	}

	@Override
	public RsServiceInitPriority getInitPriority()
	{
		return RsServiceInitPriority.NORMAL;
	}

	@Override
	public void initialize(PeerConnection peerConnection)
	{
		peerConnection.scheduleAtFixedRate(
				() -> peerConnectionManager.writeItem(peerConnection, new RttPingItem(getCounter(peerConnection), get64bitsTimeStamp()), this),
				0,
				10,
				TimeUnit.SECONDS);
	}

	private int getCounter(PeerConnection peerConnection)
	{
		var counter = (int) peerConnection.getServiceData(this, KEY_COUNTER).orElse(1);
		peerConnection.putServiceData(this, KEY_COUNTER, ++counter);
		return counter;
	}

	private static long get64bitsTimeStamp()
	{
		var now = Instant.now().truncatedTo(ChronoUnit.MICROS);

		return (now.getEpochSecond() << 32) + now.getNano() / 1_000L;
	}

	private static Instant getInstantFromTimestamp(long timestamp)
	{
		return Instant.ofEpochSecond(timestamp >> 32 & 0xffffffffL, (timestamp & 0xffffffffL) * 1_000L);
	}

	@Override
	public void handleItem(PeerConnection sender, Item item)
	{
		if (item instanceof RttPingItem pingItem)
		{
			var pong = new RttPongItem(pingItem, get64bitsTimeStamp());
			peerConnectionManager.writeItem(sender, pong, this);
		}
		else if (item instanceof RttPongItem pongItem)
		{
			var now = Instant.now();
			var ping = getInstantFromTimestamp(pongItem.getPingTimestamp());
			var pong = getInstantFromTimestamp(pongItem.getPongTimestamp());

			var rtt = Duration.between(ping, now);
			var offset = Duration.between(pong, now.minus(rtt.dividedBy(2)));
			var peerTime = now.plus(offset);

			log.debug("RTT: {}, offset: {}, peerTime: {}", rtt, offset, peerTime);

			// To calculate a mean time offset, keep ~20 of them and compute the average between them. XXX: see how RS does it and store it in the peerConnection has an extra value
		}
	}
}
