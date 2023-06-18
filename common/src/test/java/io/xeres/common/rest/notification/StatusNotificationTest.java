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

package io.xeres.common.rest.notification;

import io.xeres.common.rest.notification.status.DhtInfo;
import io.xeres.common.rest.notification.status.DhtStatus;
import io.xeres.common.rest.notification.status.NatStatus;
import io.xeres.common.rest.notification.status.StatusNotification;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class StatusNotificationTest
{
	@Test
	void StatusNotificationResponse_Equals()
	{
		var response1 = new StatusNotification(0, 1, NatStatus.UPNP, DhtInfo.fromStatus(DhtStatus.OFF));
		var response2 = new StatusNotification(0, 1, NatStatus.UPNP, DhtInfo.fromStatus(DhtStatus.OFF));
		var response3 = new StatusNotification(0, 1, NatStatus.FIREWALLED, DhtInfo.fromStatus(DhtStatus.OFF));

		assertEquals(response1, response2);
		assertNotEquals(response1, response3);
		assertNotEquals(response2, response3);
	}
}
