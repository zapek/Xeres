/*
 * Copyright (c) 2019-2020 by David Gerber - https://zapek.com
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

import io.xeres.app.database.model.profile.Profile;
import io.xeres.common.id.LocationId;
import io.xeres.common.id.ProfileFingerprint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, Long>
{
	Optional<Profile> findByName(String name);

	Optional<Profile> findByProfileFingerprint(ProfileFingerprint profileFingerprint);

	Optional<Profile> findByPgpIdentifier(long pgpIdentifier);

	@Query("SELECT p FROM Profile p, IN(p.locations) l WHERE l.locationId = :locationId")
	Optional<Profile> findProfileByLocationId(@Param("locationId") LocationId locationId);

	@Query("SELECT p FROM Profile p, IN(p.locations) l WHERE p.pgpIdentifier = :pgpIdentifier AND p.accepted = true AND p.pgpPublicKeyData is not null AND l.discoverable = true")
	Optional<Profile> findDiscoverableProfileByPgpIdentifier(@Param("pgpIdentifier") long pgpIdentifier);

	@Query("SELECT p FROM Profile p, IN(p.locations) l WHERE p.pgpIdentifier IN (:ids) AND p.accepted = true AND p.pgpPublicKeyData is not null AND l.discoverable = true")
	List<Profile> findAllDiscoverableProfilesByPgpIdentifiers(@Param("ids") Iterable<Long> ids);

	@Query("SELECT p FROM Profile p, IN(p.locations) l WHERE p.accepted = true AND p.pgpPublicKeyData is not null AND l.discoverable = true")
	List<Profile> getAllDiscoverableProfiles();

	@Query("SELECT p FROM Profile p WHERE p.pgpIdentifier IN (:ids) AND p.pgpPublicKeyData is not null")
	List<Profile> findAllCompleteByPgpIdentifiers(@Param("ids") Iterable<Long> ids);
}
