/*
 * Copyright (c) 2024-2025 by David Gerber - https://zapek.com
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

package io.xeres.app.service;

import io.xeres.app.crypto.rsa.RSA;
import io.xeres.app.database.repository.GxsIdentityRepository;
import io.xeres.app.xrs.service.identity.item.IdentityGroupItem;
import io.xeres.common.dto.identity.IdentityConstants;
import io.xeres.common.id.GxsId;
import io.xeres.common.identity.Type;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class IdentityService
{
	private final GxsIdentityRepository gxsIdentityRepository;

	public IdentityService(GxsIdentityRepository gxsIdentityRepository)
	{
		this.gxsIdentityRepository = gxsIdentityRepository;
	}

	public Optional<IdentityGroupItem> findById(long id)
	{
		return gxsIdentityRepository.findById(id);
	}

	public boolean hasOwnIdentity()
	{
		return gxsIdentityRepository.findById(IdentityConstants.OWN_IDENTITY_ID).isPresent();
	}

	public IdentityGroupItem getOwnIdentity()
	{
		return gxsIdentityRepository.findById(IdentityConstants.OWN_IDENTITY_ID).orElseThrow(() -> new IllegalStateException("Missing own gxsId"));
	}

	public List<IdentityGroupItem> findAllByName(String name)
	{
		return gxsIdentityRepository.findAllByName(name);
	}

	public Optional<IdentityGroupItem> findByGxsId(GxsId gxsId)
	{
		return gxsIdentityRepository.findByGxsId(gxsId);
	}

	public List<IdentityGroupItem> findAllByType(Type type)
	{
		return gxsIdentityRepository.findAllByType(type);
	}

	public List<IdentityGroupItem> getAll()
	{
		return gxsIdentityRepository.findAll();
	}

	public List<IdentityGroupItem> findAll(Set<GxsId> gxsIds)
	{
		return gxsIdentityRepository.findAllByGxsIdIn(gxsIds);
	}

	public List<IdentityGroupItem> findAllSubscribedAndPublishedSince(Instant since)
	{
		return gxsIdentityRepository.findAllBySubscribedIsTrueAndPublishedAfter(since);
	}

	public List<IdentityGroupItem> findAllByProfileId(long id)
	{
		return gxsIdentityRepository.findAllByProfileId(id);
	}

	@Transactional
	public IdentityGroupItem save(IdentityGroupItem identityGroupItem)
	{
		return gxsIdentityRepository.save(identityGroupItem);
	}

	public List<IdentityGroupItem> findIdentitiesToValidate(int limit)
	{
		return gxsIdentityRepository.findAllByNextValidationNotNullAndNextValidationBeforeOrderByNextValidationDesc(Instant.now(), limit <= 0 ? Limit.unlimited() : Limit.of(limit));
	}

	public void delete(IdentityGroupItem identityGroupItem)
	{
		gxsIdentityRepository.delete(identityGroupItem);
	}

	@Transactional(propagation = Propagation.NEVER)
	public byte[] signData(IdentityGroupItem identityGroupItem, byte[] data)
	{
		return RSA.sign(identityGroupItem.getAdminPrivateKey(), data);
	}

	@Transactional
	public void removeAllLinksToProfile(long profileId)
	{
		var allByProfileId = gxsIdentityRepository.findAllByProfileId(profileId);
		allByProfileId.forEach(identityGroupItem -> identityGroupItem.setProfile(null));
		// XXX: we should possibly refresh the list with contactNotificationService...
		gxsIdentityRepository.saveAll(allByProfileId);
	}
}
