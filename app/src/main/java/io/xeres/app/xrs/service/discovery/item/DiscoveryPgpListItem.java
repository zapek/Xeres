/*
 * Copyright (c) 2019-2025 by David Gerber - https://zapek.com
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

package io.xeres.app.xrs.service.discovery.item;

import io.netty.buffer.ByteBuf;
import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.item.ItemPriority;
import io.xeres.app.xrs.serialization.RsSerializable;
import io.xeres.app.xrs.serialization.SerializationFlags;
import io.xeres.app.xrs.service.RsServiceType;
import io.xeres.common.id.Id;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static io.xeres.app.xrs.serialization.Serializer.*;
import static io.xeres.app.xrs.serialization.TlvType.SET_PGP_ID;
import static java.util.stream.Collectors.joining;

public class DiscoveryPgpListItem extends Item implements RsSerializable
{
	public enum Mode
	{
		NONE,
		FRIENDS,
		GET_CERT
	}

	private Mode mode;
	private Set<Long> pgpIds = new HashSet<>();

	@SuppressWarnings("unused")
	public DiscoveryPgpListItem()
	{
	}

	public DiscoveryPgpListItem(Mode mode, Set<Long> pgpIds)
	{
		this.mode = mode;
		this.pgpIds = pgpIds;
	}

	@Override
	public int getServiceType()
	{
		return RsServiceType.GOSSIP_DISCOVERY.getType();
	}

	@Override
	public int getSubType()
	{
		return 1;
	}

	@Override
	public int getPriority()
	{
		return ItemPriority.BACKGROUND.getPriority();
	}

	public Mode getMode()
	{
		return mode;
	}

	public Set<Long> getPgpIds()
	{
		return Collections.unmodifiableSet(pgpIds);
	}

	@Override
	public int writeObject(ByteBuf buf, Set<SerializationFlags> serializationFlags)
	{
		var size = 0;

		size += serialize(buf, mode);
		size += serialize(buf, SET_PGP_ID, pgpIds);
		return size;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void readObject(ByteBuf buf)
	{
		mode = deserializeEnum(buf, Mode.class);
		pgpIds = (Set<Long>) deserialize(buf, SET_PGP_ID);
	}

	@Override
	public DiscoveryPgpListItem clone()
	{
		return (DiscoveryPgpListItem) super.clone();
	}

	@Override
	public String toString()
	{
		return "DiscoveryPgpListItem{" +
				"mode=" + mode +
				", pgpIds=" + pgpIds.stream().map(Id::toString).collect(joining(", ")) +
				'}';
	}
}
