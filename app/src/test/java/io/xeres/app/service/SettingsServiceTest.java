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

package io.xeres.app.service;

import io.xeres.app.database.model.settings.Settings;
import io.xeres.app.database.repository.SettingsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettingsServiceTest
{
	@Mock
	private SettingsRepository settingsRepository;

	@Mock
	private Settings settings;

	@InjectMocks
	private SettingsService settingsService;

	@Test
	void SaveSecretProfileKey_Success()
	{
		when(settingsRepository.findById((byte) 1)).thenReturn(Optional.of(settings));
		settingsService.init();

		settingsService.saveSecretProfileKey(new byte[]{1});

		verify(settings).setPgpPrivateKeyData(any(byte[].class));
		verify(settingsRepository).save(any(Settings.class));
	}
}
