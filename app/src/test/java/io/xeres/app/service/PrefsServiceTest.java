/*
 * Copyright (c) 2019-2021 by David Gerber - https://zapek.com
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

import io.xeres.app.database.model.prefs.Prefs;
import io.xeres.app.database.repository.PrefsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class PrefsServiceTest
{
	@Mock
	private PrefsRepository prefsRepository;

	@Mock
	private Prefs prefs;

	@InjectMocks
	private PrefsService prefsService;

	@Test
	void PrefsService_SaveSecretProfileKey_OK()
	{
		when(prefsRepository.findById((byte) 1)).thenReturn(Optional.of(prefs));
		prefsService.init();

		prefsService.saveSecretProfileKey(new byte[]{1});

		verify(prefs).setPgpPrivateKeyData(any(byte[].class));
		verify(prefsRepository).save(any(Prefs.class));
	}
}
