/*
 * Copyright (c) 2019-2025 by David Gerber - https://zapek.com
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

package io.xeres.app.service.notification.status;

import io.xeres.app.service.UiBridgeService;
import io.xeres.app.service.notification.NotificationService;
import io.xeres.common.rest.notification.Notification;
import io.xeres.common.rest.notification.status.DhtInfo;
import io.xeres.common.rest.notification.status.DhtStatus;
import io.xeres.common.rest.notification.status.NatStatus;
import io.xeres.common.rest.notification.status.StatusNotification;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;
import java.util.ResourceBundle;

@Service
public class StatusNotificationService extends NotificationService
{
	private int currentUserCount;

	private int totalUsers;

	private NatStatus natStatus = NatStatus.UNKNOWN;

	private DhtInfo dhtInfo = DhtInfo.fromStatus(DhtStatus.OFF);

	private final UiBridgeService uiBridgeService;
	private final ResourceBundle bundle;

	public StatusNotificationService(UiBridgeService uiBridgeService, ResourceBundle bundle)
	{
		super();
		this.uiBridgeService = uiBridgeService;
		this.bundle = bundle;
	}

	public void setCurrentUsersCount(int value)
	{
		currentUserCount = value;
		sendNotification(createNotification());
		uiBridgeService.setTrayStatus(MessageFormat.format(bundle.getString("main.systray.peers"), value));
	}

	public void setTotalUsers(int value)
	{
		totalUsers = value - 1; // We remove our own location
		sendNotification(createNotification());
	}

	public void setNatStatus(NatStatus value)
	{
		natStatus = value;
		sendNotification(createNotification());
	}

	public void setDhtInfo(DhtInfo value)
	{
		dhtInfo = value;
		sendNotification(createNotification());
	}

	@Override
	protected Notification initialNotification()
	{
		return createNotification();
	}

	private Notification createNotification()
	{
		return new StatusNotification(
				currentUserCount,
				totalUsers,
				natStatus,
				dhtInfo
		);
	}
}
