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

package io.xeres.app.xrs.service.mail;

import io.xeres.app.net.peer.PeerConnection;
import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.service.RsService;
import io.xeres.app.xrs.service.RsServiceRegistry;
import io.xeres.common.protocol.xrs.RsServiceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static io.xeres.common.protocol.xrs.RsServiceType.MESSAGES;

@Component
public class MailRsService extends RsService
{
	private static final Logger log = LoggerFactory.getLogger(MailRsService.class);

	protected MailRsService(RsServiceRegistry rsServiceRegistry)
	{
		super(rsServiceRegistry);
	}

	@Override
	public RsServiceType getServiceType()
	{
		return MESSAGES;
	}

	@Override
	public void handleItem(PeerConnection sender, Item item)
	{

	}
}
