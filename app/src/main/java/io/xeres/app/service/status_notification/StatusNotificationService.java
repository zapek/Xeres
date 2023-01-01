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

package io.xeres.app.service.status_notification;

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
	private final StatusNotification<Integer> currentUserCount = new StatusNotification<>(0);

	private final StatusNotification<Integer> totalUsers = new StatusNotification<>(0);

	private final StatusNotification<NatStatus> natStatus = new StatusNotification<>(NatStatus.UNKNOWN);

	private final StatusNotification<DhtInfo> dhtInfo = new StatusNotification<>(DhtInfo.fromStatus(DhtStatus.OFF));

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

	public void setCurrentUsersCount(int value)
	{
		currentUserCount.setValue(value);
		sendNotification(null);
		trayService.setTooltip(value + " peers connected");
	}

	public void setTotalUsers(int value)
	{
		totalUsers.setValue(value);
		sendNotification(null);
	}

	public void incrementTotalUsers()
	{
		totalUsers.setValue(totalUsers.getValue() + 1);
		sendNotification(null);
	}

	public void decrementTotalUsers()
	{
		if (totalUsers.getValue() > 0)
		{
			totalUsers.setValue(totalUsers.getValue() - 1);
			sendNotification(null);
		}
	}

	public void setNatStatus(NatStatus value)
	{
		natStatus.setValue(value);
		sendNotification(null);
	}

	public void setDhtInfo(DhtInfo value)
	{
		dhtInfo.setValue(value);
		sendNotification(null);
	}

	private void sendNotification(SseEmitter specificEmitter)
	{
		var newCurrentUsersCount = currentUserCount.getNewStatusIfChanged(specificEmitter);
		var newTotalUsers = totalUsers.getNewStatusIfChanged(specificEmitter);
		var newNatStatus = natStatus.getNewStatusIfChanged(specificEmitter);
		var newDhtInfo = dhtInfo.getNewStatusIfChanged(specificEmitter);

		var notification = new StatusNotificationResponse(newCurrentUsersCount, newTotalUsers, newNatStatus, newDhtInfo);

		if (notification.isEmpty())
		{
			return;
		}

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
