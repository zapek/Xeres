/*
 * Copyright (c) 2023 by David Gerber - https://zapek.com
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

import io.xeres.app.database.repository.GxsForumRepository;
import io.xeres.app.xrs.service.forum.item.ForumGroupItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ForumService
{
	private static final Logger log = LoggerFactory.getLogger(ForumService.class);

	private final GxsForumRepository gxsForumRepository;

	public ForumService(GxsForumRepository gxsForumRepository)
	{
		this.gxsForumRepository = gxsForumRepository;
	}

	public List<ForumGroupItem> findAll()
	{
		return gxsForumRepository.findAll();
	}
}
