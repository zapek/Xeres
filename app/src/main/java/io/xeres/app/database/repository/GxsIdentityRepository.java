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

import io.xeres.app.xrs.service.identity.item.IdentityGroupItem;
import io.xeres.common.id.GxsId;
import io.xeres.common.identity.Type;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface GxsIdentityRepository extends JpaRepository<IdentityGroupItem, Long>
{
	Optional<IdentityGroupItem> findByGxsId(GxsId gxsId);

	List<IdentityGroupItem> findAllByGxsIdIn(Set<GxsId> gxsIds);

	List<IdentityGroupItem> findAllByName(String name);

	List<IdentityGroupItem> findAllByType(Type type);

	List<IdentityGroupItem> findAllByPublishedAfter(Instant since);
}
