package io.xeres.app.database.repository;

import io.xeres.app.database.model.file.FileFakes;
import io.xeres.common.file.FileType;
import io.xeres.testutils.Sha1SumFakes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class FileRepositoryTest
{
	@Autowired
	private FileRepository fileRepository;

	@Test
	void FileRepository_CRUD_OK()
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
	void FileRepository_FindByHash_OK()
	{
		var hash = Sha1SumFakes.createSha1Sum();
		var file = FileFakes.createFile("foo", null);
		file.setHash(hash);
		fileRepository.save(file);

		var found = fileRepository.findByHash(hash).getFirst();
		assertNotNull(found);
	}

	@Test
	void FileRepository_FindByEncryptedHash_OK()
	{
		var hash = Sha1SumFakes.createSha1Sum();
		var file = FileFakes.createFile("foo", null);
		file.setEncryptedHash(hash);
		fileRepository.save(file);

		var found = fileRepository.findByEncryptedHash(hash).getFirst();
		assertNotNull(found);
	}
}
