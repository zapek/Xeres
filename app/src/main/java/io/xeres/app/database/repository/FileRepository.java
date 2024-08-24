/*
 * Copyright (c) 2024 by David Gerber - https://zapek.com
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

import io.xeres.app.database.model.file.File;
import io.xeres.common.id.Sha1Sum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Transactional(readOnly = true)
public interface FileRepository extends JpaRepository<File, Long>
{
	List<File> findAllByName(String name);

	List<File> findAllByNameContainingIgnoreCase(String name);

	Optional<File> findByNameAndParent(String name, File parent);

	Optional<File> findByNameAndParentName(String name, String parentName);

	int countByParent(File parent);

	List<File> findByHash(Sha1Sum hash);

	List<File> findByEncryptedHash(Sha1Sum encryptedHash);
}
