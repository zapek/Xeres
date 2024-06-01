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

package io.xeres.app.database.repository;

import io.xeres.app.database.model.gxs.GxsServiceSettingFakes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class GxsServiceSettingRepositoryTest
{
	@Autowired
	private GxsServiceSettingRepository gxsServiceSettingRepository;

	@Test
	void GxsServiceSettingRepository_CRUD_OK()
	{
		var instantEpoch = Instant.EPOCH;
		var instantNow = Instant.now();
		var instantYesterday = instantNow.minus(1, ChronoUnit.DAYS);

		var gxsServiceSetting1 = GxsServiceSettingFakes.createGxsServiceSetting(1, instantEpoch);
		var gxsServiceSetting2 = GxsServiceSettingFakes.createGxsServiceSetting(2, instantYesterday);
		var gxsServiceSetting3 = GxsServiceSettingFakes.createGxsServiceSetting(3, instantNow);

		var savedGxsServiceSetting1 = gxsServiceSettingRepository.save(gxsServiceSetting1);
		var savedGxsServiceSetting2 = gxsServiceSettingRepository.save(gxsServiceSetting2);
		gxsServiceSettingRepository.save(gxsServiceSetting3);

		var gxsServiceSettings = gxsServiceSettingRepository.findAll();
		assertNotNull(gxsServiceSettings);
		assertEquals(3, gxsServiceSettings.size());

		var first = gxsServiceSettingRepository.findById(gxsServiceSettings.getFirst().getId()).orElse(null);
		assertNotNull(first);
		assertEquals(savedGxsServiceSetting1.getId(), first.getId());
		assertEquals(savedGxsServiceSetting1.getLastUpdated(), first.getLastUpdated());

		var second = gxsServiceSettingRepository.findById(savedGxsServiceSetting2.getId()).orElse(null);
		assertNotNull(second);
		assertEquals(savedGxsServiceSetting2.getId(), second.getId());
		assertEquals(savedGxsServiceSetting2.getLastUpdated(), second.getLastUpdated());

		first.setLastUpdated(instantNow.plus(1, ChronoUnit.DAYS));

		var updatedGxsServiceSetting = gxsServiceSettingRepository.save(first);

		assertNotNull(updatedGxsServiceSetting);
		assertEquals(first.getId(), updatedGxsServiceSetting.getId());
		assertEquals(instantNow.plus(1, ChronoUnit.DAYS), updatedGxsServiceSetting.getLastUpdated());

		gxsServiceSettingRepository.deleteById(first.getId());

		var deleted = gxsServiceSettingRepository.findById(first.getId());
		assertTrue(deleted.isEmpty());
	}
}
