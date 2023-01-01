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

package io.xeres.app.database.model.gxs;

import io.xeres.app.xrs.service.forum.item.ForumGroupItem;
import io.xeres.common.id.GxsId;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;

import java.time.Instant;
import java.util.EnumSet;

public final class ForumGroupItemFakes
{
	private ForumGroupItemFakes()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static ForumGroupItem createForumGroupItem()
	{
		return createForumGroupItem(new GxsId(RandomUtils.nextBytes(16)), RandomStringUtils.randomAlphabetic(8));
	}

	public static ForumGroupItem createForumGroupItem(GxsId gxsId, String name)
	{
		var item = new ForumGroupItem(gxsId, name);
		item.setDiffusionFlags(EnumSet.noneOf(GxsPrivacyFlags.class));
		item.setSignatureFlags(EnumSet.noneOf(GxsSignatureFlags.class));
		item.setPublished(Instant.now());
		item.setDescription("blabla");
		return item;
	}
}
