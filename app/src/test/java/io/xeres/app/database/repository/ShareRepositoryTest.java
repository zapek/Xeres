/*
 * Copyright (c) 2026 by David Gerber - https://zapek.com
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

import io.xeres.app.database.model.file.FileFakes;
import io.xeres.app.database.model.share.Share;
import io.xeres.common.pgp.Trust;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class ShareRepositoryTest
{
	@Autowired
	private ShareRepository shareRepository;

	private static Share createShare(String name)
	{
		return Share.createShare(name, FileFakes.createFile(name), true, Trust.FULL);
	}

	@Test
	void CRUD_Success()
	{
		var share1 = createShare("foo");
		var share2 = createShare("bar");
		var share3 = createShare("plop");

		var savedShare1 = shareRepository.save(share1);
		shareRepository.save(share2);
		shareRepository.save(share3);

		var shares = shareRepository.findAll();
		assertNotNull(shares);
		assertEquals(3, shares.size());

		var first = shareRepository.findById(shares.getFirst().getId()).orElse(null);

		assertNotNull(first);
		assertEquals(savedShare1.getId(), first.getId());
		assertEquals(savedShare1.getName(), first.getName());

		first.setSearchable(false);

		var updatedShare = shareRepository.save(first);

		assertNotNull(updatedShare);
		assertEquals(first.getId(), updatedShare.getId());
		assertFalse(updatedShare.isSearchable());

		shareRepository.deleteById(first.getId());

		var deleted = shareRepository.findById(first.getId());
		assertTrue(deleted.isEmpty());
	}

	@Test
	void FindByName_Success()
	{
		var share = createShare("foo");
		shareRepository.save(share);

		var found = shareRepository.findByName("foo").orElse(null);
		assertNotNull(found);
		assertEquals(share.getName(), found.getName());

		assertTrue(shareRepository.findByName("bar").isEmpty());
	}

	@Test
	void FindShareByFileIdIn_Success()
	{
		var share1 = createShare("foo");
		var share2 = createShare("bar");

		shareRepository.save(share1);
		shareRepository.save(share2);

		var fileId = share2.getFile().getId();

		var found = shareRepository.findShareByFileIdIn(Set.of(fileId)).orElse(null);
		assertNotNull(found);
		assertEquals(share2.getName(), found.getName());
	}

	@Test
	void FindShareByFile_Success()
	{
		var share = createShare("foo");
		shareRepository.save(share);

		var found = shareRepository.findShareByFile(share.getFile()).orElse(null);
		assertNotNull(found);
		assertEquals(share.getName(), found.getName());
	}
}
