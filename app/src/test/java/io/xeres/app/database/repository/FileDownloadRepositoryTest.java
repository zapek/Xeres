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

import io.xeres.app.database.model.file.FileDownload;
import io.xeres.app.database.model.location.Location;
import io.xeres.app.database.model.location.LocationFakes;
import io.xeres.app.database.model.profile.ProfileFakes;
import io.xeres.common.id.Sha1Sum;
import io.xeres.testutils.Sha1SumFakes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.BitSet;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class FileDownloadRepositoryTest
{
	@Autowired
	private ProfileRepository profileRepository;

	@Autowired
	private LocationRepository locationRepository;

	@Autowired
	private FileDownloadRepository fileDownloadRepository;

	@Test
	void CRUD_Success()
	{
		var profile = ProfileFakes.createFreshProfile("test", 1);
		var savedProfile = profileRepository.save(profile);

		var location = LocationFakes.createFreshLocation("test1", savedProfile);
		var savedLocation = locationRepository.save(location);

		var fileDownload1 = createFileDownload("foo", Sha1SumFakes.createSha1Sum(), null);
		var fileDownload2 = createFileDownload("bar", Sha1SumFakes.createSha1Sum(), null);
		var fileDownload3 = createFileDownload("plop", Sha1SumFakes.createSha1Sum(), savedLocation);

		fileDownload3.setCompleted(true);

		var savedFileDownload1 = fileDownloadRepository.save(fileDownload1);
		fileDownloadRepository.save(fileDownload2);
		fileDownloadRepository.save(fileDownload3);

		var fileDownloads = fileDownloadRepository.findAll();
		assertNotNull(fileDownloads);
		assertEquals(3, fileDownloads.size());

		var first = fileDownloadRepository.findById(fileDownloads.getFirst().getId()).orElse(null);

		assertNotNull(first);
		assertEquals(savedFileDownload1.getId(), first.getId());
		assertEquals(savedFileDownload1.getName(), first.getName());

		first.setSize(1234L);

		var updatedFileDownload = fileDownloadRepository.save(first);

		assertNotNull(updatedFileDownload);
		assertEquals(first.getId(), updatedFileDownload.getId());
		assertEquals(1234L, updatedFileDownload.getSize());

		fileDownloadRepository.deleteById(first.getId());

		var deleted = fileDownloadRepository.findById(first.getId());
		assertTrue(deleted.isEmpty());
	}

	@Test
	void FindByHash_Success()
	{
		var hash = Sha1SumFakes.createSha1Sum();
		var fileDownload = createFileDownload("foo", hash, null);
		fileDownloadRepository.save(fileDownload);

		var found = fileDownloadRepository.findByHash(hash).orElse(null);

		assertNotNull(found);
		assertEquals(hash, found.getHash());
	}

	@Test
	void FindByHash_NotFound_Success()
	{
		assertTrue(fileDownloadRepository.findByHash(Sha1SumFakes.createSha1Sum()).isEmpty());
	}

	@Test
	void FindAllByLocationIsNull_Success()
	{
		var withoutLocation1 = createFileDownload("foo", Sha1SumFakes.createSha1Sum(), null);
		var withoutLocation2 = createFileDownload("bar", Sha1SumFakes.createSha1Sum(), null);

		fileDownloadRepository.save(withoutLocation1);
		fileDownloadRepository.save(withoutLocation2);

		var found = fileDownloadRepository.findAllByLocationIsNull();

		assertNotNull(found);
		assertEquals(2, found.size());
	}

	@Test
	void FindAllByLocation_Success()
	{
		var profile = ProfileFakes.createFreshProfile("test", 1);
		var savedProfile = profileRepository.save(profile);

		var location = LocationFakes.createFreshLocation("test1", savedProfile);
		var savedLocation = locationRepository.save(location);

		var withLocation = createFileDownload("foo", Sha1SumFakes.createSha1Sum(), savedLocation);
		var withoutLocation = createFileDownload("bar", Sha1SumFakes.createSha1Sum(), null);

		fileDownloadRepository.save(withLocation);
		fileDownloadRepository.save(withoutLocation);

		var found = fileDownloadRepository.findAllByLocation(savedLocation);

		assertNotNull(found);
		assertEquals(1, found.size());
		assertTrue(found.getFirst().hasLocation());
	}

	@Test
	void DeleteAllByCompletedTrue_Success()
	{
		var uncompleted = createFileDownload("foo", Sha1SumFakes.createSha1Sum(), null);
		var completed1 = createFileDownload("bar", Sha1SumFakes.createSha1Sum(), null);
		completed1.setCompleted(true);
		var completed2 = createFileDownload("plop", Sha1SumFakes.createSha1Sum(), null);
		completed2.setCompleted(true);

		fileDownloadRepository.save(uncompleted);
		fileDownloadRepository.save(completed1);
		fileDownloadRepository.save(completed2);

		fileDownloadRepository.deleteAllByCompletedTrue();

		var remaining = fileDownloadRepository.findAll();

		assertEquals(1, remaining.size());
		assertFalse(remaining.getFirst().isCompleted());
	}

	private static FileDownload createFileDownload(String name, Sha1Sum hash, Location location)
	{
		var fileDownload = new FileDownload();
		fileDownload.setName(name);
		fileDownload.setSize(1024L);
		fileDownload.setHash(hash);
		fileDownload.setChunkMap(new BitSet());
		if (location != null)
		{
			fileDownload.setLocation(location);
		}
		return fileDownload;
	}
}
