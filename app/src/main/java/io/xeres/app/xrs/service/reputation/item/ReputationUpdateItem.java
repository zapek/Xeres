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

package io.xeres.app.xrs.service.reputation.item;

import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.serialization.RsSerialized;
import io.xeres.common.id.GxsId;
import io.xeres.common.protocol.xrs.RsServiceType;

import java.util.Map;

public class ReputationUpdateItem extends Item
{
	@RsSerialized
	private int latestUpdate;

	@RsSerialized
	private Map<GxsId, Integer> opinions;

	@SuppressWarnings("unused")
	public ReputationUpdateItem()
	{
	}

	public ReputationUpdateItem(int latestUpdate, Map<GxsId, Integer> opinions)
	{
		this.latestUpdate = latestUpdate;
		this.opinions = opinions;
	}

	@Override
	public int getServiceType()
	{
		return RsServiceType.GXS_REPUTATION.getType();
	}

	@Override
	public int getSubType()
	{
		return 3;
	}

	public int getLatestUpdate()
	{
		return latestUpdate;
	}

	public Map<GxsId, Integer> getOpinions()
	{
		return opinions;
	}

	@Override
	public ReputationUpdateItem clone()
	{
		return (ReputationUpdateItem) super.clone();
	}

	@Override
	public String toString()
	{
		return "ReputationUpdateItem{" +
				"latestUpdate=" + latestUpdate +
				", opinions=" + opinions +
				'}';
	}
}
