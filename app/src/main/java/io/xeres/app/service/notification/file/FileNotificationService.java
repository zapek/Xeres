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
import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Service
public class FileNotificationService extends NotificationService
{
	private String shareName;
	private String scannedFile;

	@Override
	protected Notification createNotification()
	{
		return new FileNotification(shareName, scannedFile);
	}

	public void startScanning(Share share)
	{
		this.shareName = share.getName();
		sendNotification();
	}

	public void setScanningFile(Path scannedFile)
	{
		this.scannedFile = scannedFile != null ? scannedFile.toString() : null;
		sendNotification();
	}

	public void stopScanning()
	{
		this.shareName = null;
		this.scannedFile = null;
		sendNotification();
	}
}
