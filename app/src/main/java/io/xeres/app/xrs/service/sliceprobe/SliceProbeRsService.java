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

package io.xeres.app.xrs.service.sliceprobe;

import io.xeres.app.net.peer.PeerAttribute;
import io.xeres.app.net.peer.PeerConnection;
import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.service.RsService;
import io.xeres.app.xrs.service.RsServiceType;
import io.xeres.app.xrs.service.sliceprobe.item.SliceProbeItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Map;

import static io.xeres.app.xrs.service.RsServiceType.PACKET_SLICING_PROBE;

@Component
public class SliceProbeRsService extends RsService
{
	private static final Logger log = LoggerFactory.getLogger(SliceProbeRsService.class);

	public SliceProbeRsService(Environment environment)
	{
		super(environment);
	}

	@Override
	public RsServiceType getServiceType()
	{
		return PACKET_SLICING_PROBE;
	}

	@Override
	public Map<Class<? extends Item>, Integer> getSupportedItems()
	{
		return Map.of(
				SliceProbeItem.class, 0xCC
		);
	}

	@Override
	public void handleItem(PeerConnection sender, Item item)
	{
		if (!Boolean.TRUE.equals(sender.getCtx().channel().attr(PeerAttribute.MULTI_PACKET).get()))
		{
			log.debug("Received slice probe, switching to new packet format for current session");
			sender.getCtx().channel().attr(PeerAttribute.MULTI_PACKET).set(true);
		}
	}
}
