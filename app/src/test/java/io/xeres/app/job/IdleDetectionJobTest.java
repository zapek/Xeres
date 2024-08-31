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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static io.xeres.common.location.Availability.AVAILABLE;
import static io.xeres.common.location.Availability.AWAY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class IdleDetectionJobTest
{
	@Mock
	private PeerService peerService;

	@Mock
	private StatusRsService statusRsService;

	@Mock
	private IdleChecker idleChecker;

	@InjectMocks
	private IdleDetectionJob idleDetectionJob;

	@Test
	void IsOnline_Success()
	{
		when(peerService.isRunning()).thenReturn(true);
		when(idleChecker.getIdleTime()).thenReturn(0);

		idleDetectionJob.checkIdle();

		verify(statusRsService).changeAvailability(argThat(status -> {
			assertEquals(AVAILABLE, status);
			return true;
		}));
	}

	@Test
	void IsAway_Success()
	{
		when(peerService.isRunning()).thenReturn(true);
		when(idleChecker.getIdleTime()).thenReturn(60 * 5 + 1);

		idleDetectionJob.checkIdle();

		verify(statusRsService).changeAvailability(argThat(status -> {
			assertEquals(AWAY, status);
			return true;
		}));
	}
}
