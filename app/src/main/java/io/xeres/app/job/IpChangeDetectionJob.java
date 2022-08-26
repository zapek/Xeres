/*
 * Copyright (c) 2019-2022 by David Gerber - https://zapek.com
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

import io.xeres.app.application.events.IpChangedEvent;
import io.xeres.app.service.PeerService;
import io.xeres.app.service.SettingsService;
import io.xeres.common.protocol.ip.IP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class IpChangeDetectionJob
{
	private static final Logger log = LoggerFactory.getLogger(IpChangeDetectionJob.class);

	private final PeerService peerService;
	private final SettingsService settingsService;
	private final ApplicationEventPublisher publisher;


	public IpChangeDetectionJob(PeerService peerService, SettingsService settingsService, ApplicationEventPublisher publisher)
	{
		this.peerService = peerService;
		this.settingsService = settingsService;
		this.publisher = publisher;
	}

	@Scheduled(initialDelay = 2, fixedDelay = 1, timeUnit = TimeUnit.MINUTES)
	void checkIp()
	{
		// Do not check if the network is not up
		if (!peerService.isRunning())
		{
			return;
		}

		var localIpAddress = IP.getLocalIpAddress();
		if (localIpAddress == null)
		{
			log.error("No local IP address detected on the host. Waiting...");
			return;
		}

		if (!localIpAddress.equals(settingsService.getLocalIpAddress()))
		{
			log.warn("Local IP address changed: {} -> {}", settingsService.getLocalIpAddress(), localIpAddress);
			publisher.publishEvent(new IpChangedEvent(localIpAddress));
		}
	}
}
