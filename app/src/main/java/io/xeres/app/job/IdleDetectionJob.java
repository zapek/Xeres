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

package io.xeres.app.job;

import io.xeres.app.service.PeerService;
import io.xeres.app.xrs.service.status.IdleChecker;
import io.xeres.app.xrs.service.status.StatusRsService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

import static io.xeres.common.location.Availability.AVAILABLE;
import static io.xeres.common.location.Availability.AWAY;

/**
 * This job changes the status of the user to away or online depending on
 * if he's idle or not.
 */
@Component
public class IdleDetectionJob
{
	private static final long IDLE_TIME_MINUTES = 5;

	private final StatusRsService statusRsService;
	private final PeerService peerService;
	private final IdleChecker idleChecker;

	public IdleDetectionJob(StatusRsService statusRsService, PeerService peerService, IdleChecker idleChecker)
	{
		this.statusRsService = statusRsService;
		this.peerService = peerService;
		this.idleChecker = idleChecker;
	}

	@Scheduled(initialDelay = 5 * 60, fixedDelay = 5, timeUnit = TimeUnit.SECONDS)
	void checkIdle()
	{
		if (!JobUtils.canRun(peerService))
		{
			return;
		}

		var idleTime = idleChecker.getIdleTime();
		if (idleTime < TimeUnit.MINUTES.toSeconds(IDLE_TIME_MINUTES))
		{
			statusRsService.changeAvailabilityAutomatically(AVAILABLE);
		}
		else
		{
			statusRsService.changeAvailabilityAutomatically(AWAY);
		}
	}
}
