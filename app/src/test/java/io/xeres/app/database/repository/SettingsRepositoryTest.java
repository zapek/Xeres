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

package io.xeres.app.database.repository;

import io.xeres.app.database.model.settings.SettingsFakes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class SettingsRepositoryTest
{
	@Autowired
	private SettingsRepository settingsRepository;

	@Test
	void CRUD_Success()
	{
		var prefs = SettingsFakes.createSettings();
		var unwantedPrefs = SettingsFakes.createSettings();

		var savedPrefs = settingsRepository.save(prefs);
		settingsRepository.save(unwantedPrefs);

		var prefsList = settingsRepository.findAll();
		assertNotNull(prefsList);
		assertEquals(1, prefsList.size());

		var first = settingsRepository.findById((byte) 1).orElse(null);

		assertNotNull(first);
		assertArrayEquals(savedPrefs.getPgpPrivateKeyData(), first.getPgpPrivateKeyData());

		first.setPgpPrivateKeyData(new byte[]{1});

		var updatedPrefs = settingsRepository.save(first);

		assertNotNull(updatedPrefs);
		assertArrayEquals(first.getPgpPrivateKeyData(), updatedPrefs.getPgpPrivateKeyData());

		settingsRepository.deleteById((byte) 1);

		var deleted = settingsRepository.findById((byte) 1);
		assertTrue(deleted.isEmpty());

		// And then save again to make sure the ID stays at 1
		settingsRepository.save(prefs);

		prefsList = settingsRepository.findAll();
		assertNotNull(prefsList);
		assertEquals(1, prefsList.size());
	}
}
