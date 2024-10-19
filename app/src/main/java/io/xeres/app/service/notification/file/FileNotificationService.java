/*
 * Copyright (c) 2023 by David Gerber - https://zapek.com
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

package io.xeres.app.service.notification.file;

import io.xeres.app.database.model.share.Share;
import io.xeres.app.service.notification.NotificationService;
import io.xeres.common.rest.notification.Notification;
import io.xeres.common.rest.notification.file.FileNotification;
import io.xeres.common.rest.notification.file.FileNotificationAction;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

import static io.xeres.common.rest.notification.file.FileNotificationAction.*;

@Service
public class FileNotificationService extends NotificationService
{
	private FileNotificationAction action = NONE;
	private String shareName;
	private String scannedFile;

	@Override
	protected Notification initialNotification()
	{
		return createNotification();
	}

	private Notification createNotification()
	{
		return new FileNotification(action, shareName, scannedFile);
	}

	public void startScanning(Share share)
	{
		action = START_SCANNING;
		shareName = share.getName();
		sendNotification(createNotification());
	}

	public void startScanningFile(Path scannedFile)
	{
		action = START_HASHING;
		this.scannedFile = scannedFile.toString();
		sendNotification(createNotification());
	}

	public void stopScanningFile()
	{
		action = STOP_HASHING;
		scannedFile = null;
		sendNotification(createNotification());
	}

	public void stopScanning()
	{
		action = STOP_SCANNING;
		shareName = null;
		scannedFile = null;
		sendNotification(createNotification());
	}
}
