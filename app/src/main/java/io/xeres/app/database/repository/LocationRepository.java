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

import io.xeres.app.database.model.location.Location;
import io.xeres.common.id.LocationIdentifier;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Transactional(readOnly = true)
public interface LocationRepository extends JpaRepository<Location, Long>
{
	Optional<Location> findByLocationIdentifier(LocationIdentifier locationIdentifier);

	Slice<Location> findAllByConnectedFalse(Pageable pageable);

	Slice<Location> findAllByConnectedFalseAndDhtTrue(Pageable pageable);

	List<Location> findAllByConnectedTrue();

	@Modifying
	@Transactional
	@Query("UPDATE Location l SET l.connected = false WHERE l.connected = true")
	void putAllConnectedToFalse();
}
