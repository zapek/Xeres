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

package io.xeres.app.application.autostart;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class AutoStartTest
{
	@Mock
	private AutoStarter autoStarter;

	@InjectMocks
	private AutoStart autoStart;

	@Test
	void Enable_Supported_Success()
	{
		when(autoStarter.isSupported()).thenReturn(true);

		autoStart.enable();

		verify(autoStarter).enable();
	}

	@Test
	void Enable_NotSupported_NoOp()
	{
		when(autoStarter.isSupported()).thenReturn(false);

		autoStart.enable();

		verify(autoStarter, times(0)).enable();
	}

	@Test
	void Disable_Supported_Success()
	{
		when(autoStarter.isSupported()).thenReturn(true);

		autoStart.disable();

		verify(autoStarter).disable();
	}

	@Test
	void Disable_NotSupported_NoOp()
	{
		when(autoStarter.isSupported()).thenReturn(false);

		autoStart.disable();

		verify(autoStarter, times(0)).disable();
	}

	@Test
	void IsEnabled_Supported_Success()
	{
		when(autoStarter.isSupported()).thenReturn(true);
		when(autoStarter.isEnabled()).thenReturn(true);

		var enabled = autoStart.isEnabled();

		assertTrue(enabled);

		verify(autoStarter).isEnabled();
	}

	@Test
	void IsEnabled_NotSupported_False()
	{
		when(autoStarter.isSupported()).thenReturn(false);
		when(autoStarter.isEnabled()).thenReturn(true);

		var enabled = autoStart.isEnabled();

		assertFalse(enabled);

		verify(autoStarter, times(0)).isEnabled();
	}
}
