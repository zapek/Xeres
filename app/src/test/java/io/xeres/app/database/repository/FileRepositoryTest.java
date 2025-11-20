/*
 * Copyright (c) 2025 by David Gerber - https://zapek.com
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
import io.xeres.common.file.FileType;
import io.xeres.testutils.Sha1SumFakes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class FileRepositoryTest
{
	@Autowired
	private FileRepository fileRepository;

	@Test
	void CRUD_Success()
	{
		var file1 = FileFakes.createFile("foo", null);
		var file2 = FileFakes.createFile("bar", null);
		var file3 = FileFakes.createFile("plop", null);

		var savedFile = fileRepository.save(file1);
		fileRepository.save(file2);
		fileRepository.save(file3);

		var files = fileRepository.findAll();
		assertNotNull(files);
		assertEquals(3, files.size());

		var first = fileRepository.findById(files.getFirst().getId()).orElse(null);

		assertNotNull(first);
		assertEquals(savedFile.getId(), first.getId());
		assertEquals(savedFile.getName(), first.getName());

		first.setType(FileType.VIDEO);

		var updatedFile = fileRepository.save(first);

		assertNotNull(updatedFile);
		assertEquals(first.getId(), updatedFile.getId());
		assertEquals(FileType.VIDEO, updatedFile.getType());

		fileRepository.deleteById(first.getId());

		var deleted = fileRepository.findById(first.getId());
		assertTrue(deleted.isEmpty());
	}

	@Test
	void FindByHash_Success()
	{
		var hash = Sha1SumFakes.createSha1Sum();
		var file = FileFakes.createFile("foo", null);
		file.setHash(hash);
		fileRepository.save(file);

		var found = fileRepository.findByHash(hash).getFirst();
		assertNotNull(found);
	}

	@Test
	void FindByEncryptedHash_Success()
	{
		var hash = Sha1SumFakes.createSha1Sum();
		var file = FileFakes.createFile("foo", null);
		file.setEncryptedHash(hash);
		fileRepository.save(file);

		var found = fileRepository.findByEncryptedHash(hash).getFirst();
		assertNotNull(found);
	}
}
