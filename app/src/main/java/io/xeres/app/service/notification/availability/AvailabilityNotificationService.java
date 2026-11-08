/*
 * Copyright (c) 2024-2026 by David Gerber - https://zapek.com
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

package io.xeres.app.service.notification.availability;

import io.xeres.app.database.model.location.Location;
import io.xeres.app.service.notification.NotificationService;
import io.xeres.common.location.Availability;
import io.xeres.common.rest.notification.availability.AvailabilityChange;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class AvailabilityNotificationService extends NotificationService
{
	public AvailabilityNotificationService(ApplicationEventPublisher publisher)
	{
		super(publisher);
	}

	public void changeAvailability(Location location, Availability availability)
	{
		publish(new AvailabilityChange(availability, location.getProfile().getId(), location.getProfile().getName(), location.getId(), location.getSafeName()));
	}

	@TransactionalEventListener(fallbackExecution = true)
	public void onAvailabilityChanged(AvailabilityChange notification)
	{
		sendNotification(notification);
	}
}
