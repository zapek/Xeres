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

package io.xeres.app.database.repository;

import io.xeres.app.database.model.prefs.Prefs;
import io.xeres.app.database.model.prefs.PrefsFakes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class PrefsRepositoryTest
{
	@Autowired
	private PrefsRepository prefsRepository;

	@Test
	void PrefsRepository_CRUD_OK()
	{
		Prefs prefs = PrefsFakes.createPrefs();
		Prefs unwantedPrefs = PrefsFakes.createPrefs();

		Prefs savedPrefs = prefsRepository.save(prefs);
		prefsRepository.save(unwantedPrefs);

		List<Prefs> prefsList = prefsRepository.findAll();
		assertNotNull(prefsList);
		assertEquals(1, prefsList.size());

		Prefs first = prefsRepository.findById((byte) 1).orElse(null);

		assertNotNull(first);
		assertArrayEquals(savedPrefs.getPgpPrivateKeyData(), first.getPgpPrivateKeyData());

		first.setPgpPrivateKeyData(new byte[]{1});

		Prefs updatedPrefs = prefsRepository.save(first);

		assertNotNull(updatedPrefs);
		assertArrayEquals(first.getPgpPrivateKeyData(), updatedPrefs.getPgpPrivateKeyData());

		prefsRepository.deleteById((byte) 1);

		Optional<Prefs> deleted = prefsRepository.findById((byte) 1);
		assertTrue(deleted.isEmpty());

		// And then save again to make sure the ID stays at 1
		prefsRepository.save(prefs);

		prefsList = prefsRepository.findAll();
		assertNotNull(prefsList);
		assertEquals(1, prefsList.size());
	}
}
