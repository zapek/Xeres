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

package io.xeres.app.service;

import io.xeres.app.api.sse.SsePushNotificationService;
import io.xeres.common.rest.notification.DhtInfo;
import io.xeres.common.rest.notification.DhtStatus;
import io.xeres.common.rest.notification.NatStatus;
import io.xeres.common.rest.notification.StatusNotificationResponse;
import io.xeres.ui.support.tray.TrayService;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class StatusNotificationService
{
	private int currentUsersCount;
	private boolean currentUsersCountChanged;
	private int totalUsers;
	private boolean totalUsersChanged;

	private NatStatus natStatus = NatStatus.UNKNOWN;
	private boolean natStatusChanged;

	private DhtInfo dhtInfo = DhtInfo.fromStatus(DhtStatus.OFF);
	private boolean dhtInfoChanged;

	private final SsePushNotificationService ssePushNotificationService;
	private final TrayService trayService;

	public StatusNotificationService(SsePushNotificationService ssePushNotificationService, TrayService trayService)
	{
		this.ssePushNotificationService = ssePushNotificationService;
		this.trayService = trayService;
	}

	public SseEmitter addClient()
	{
		var emitter = new SseEmitter(-1L); // no timeout
		ssePushNotificationService.addEmitter(emitter);
		emitter.onCompletion(() -> ssePushNotificationService.removeEmitter(emitter));
		emitter.onTimeout(() -> ssePushNotificationService.removeEmitter(emitter));

		CompletableFuture.delayedExecutor(1, TimeUnit.SECONDS).execute(() -> sendNotification(emitter)); // send a notification to the client that just connected to "sync" it

		return emitter;
	}

	public void setCurrentUsersCount(int currentUsersCount)
	{
		this.currentUsersCount = currentUsersCount;
		currentUsersCountChanged = true;
		sendNotification(null);
		trayService.setTooltip(currentUsersCount + " peers connected");
	}

	public void setTotalUsers(int totalUsers)
	{
		this.totalUsers = totalUsers;
		totalUsersChanged = true;
		sendNotification(null);
	}

	public void setNatStatus(NatStatus natStatus)
	{
		this.natStatus = natStatus;
		natStatusChanged = true;
		sendNotification(null);
	}

	public void setDhtInfo(DhtInfo dhtInfo)
	{
		this.dhtInfo = dhtInfo;
		dhtInfoChanged = true;
		sendNotification(null);
	}

	private void sendNotification(SseEmitter specificEmitter)
	{
		Integer newCurrentUsersCount = null;
		if (currentUsersCountChanged || specificEmitter != null)
		{
			newCurrentUsersCount = currentUsersCount;
			if (specificEmitter == null)
			{
				currentUsersCountChanged = false;
			}
		}

		Integer newTotalUsers = null;
		if (totalUsersChanged || specificEmitter != null)
		{
			newTotalUsers = totalUsers;
			if (specificEmitter == null)
			{
				totalUsersChanged = false;
			}
		}

		NatStatus newNatStatus = null;
		if (natStatusChanged || specificEmitter != null)
		{
			newNatStatus = natStatus;
			if (specificEmitter == null)
			{
				natStatusChanged = false;
			}
		}

		DhtInfo newDhtInfo = null;
		if (dhtInfoChanged || specificEmitter != null)
		{
			newDhtInfo = dhtInfo;
			if (specificEmitter == null)
			{
				dhtInfoChanged = false;
			}
		}

		var notification = new StatusNotificationResponse(newCurrentUsersCount, newTotalUsers, newNatStatus, newDhtInfo);

		if (specificEmitter != null)
		{
			ssePushNotificationService.sendNotification(specificEmitter, notification);
		}
		else
		{
			ssePushNotificationService.sendNotification(notification);
		}
	}
}
