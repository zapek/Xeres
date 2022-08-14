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

import io.xeres.app.xrs.service.status.IdleChecker;
import io.xeres.app.xrs.service.status.StatusRsService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

import static io.xeres.app.xrs.service.status.item.StatusItem.Status.AWAY;
import static io.xeres.app.xrs.service.status.item.StatusItem.Status.ONLINE;

@Component
public class IdleDetectionJob
{
	private final StatusRsService statusRsService;
	private final IdleChecker idleChecker;

	public IdleDetectionJob(StatusRsService statusRsService, IdleChecker idleChecker)
	{
		this.statusRsService = statusRsService;
		this.idleChecker = idleChecker;
	}

	@Scheduled(initialDelay = 5 * 60, fixedDelay = 5, timeUnit = TimeUnit.SECONDS)
	void checkIdle()
	{
		var idleTime = idleChecker.getIdleTime();
		if (idleTime < TimeUnit.MINUTES.toSeconds(5))
		{
			statusRsService.changeStatus(ONLINE);
		}
		else
		{
			statusRsService.changeStatus(AWAY);
		}
	}
}
