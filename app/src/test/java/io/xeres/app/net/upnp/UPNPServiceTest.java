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

package io.xeres.app.net.upnp;

import io.xeres.app.service.notification.status.StatusNotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Duration;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertFalse;

@ExtendWith(SpringExtension.class)
class UPNPServiceTest
{
	@Mock
	private StatusNotificationService statusNotificationService;

	@InjectMocks
	private UPNPService upnpService;

	@Test
	void StartStop_Success()
	{
		upnpService.start("127.0.0.1", 1901, 0); // nothing should reply in there
		await().atMost(Duration.ofSeconds(2)).until(() -> upnpService.isRunning());

		upnpService.stop();
		upnpService.waitForTermination();
		assertFalse(upnpService.isRunning());
	}
}
