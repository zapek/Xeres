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
import io.xeres.app.service.MailService;
import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.service.RsService;
import io.xeres.app.xrs.service.RsServiceConstants;
import io.xeres.app.xrs.service.RsServiceRegistry;
import io.xeres.app.xrs.service.mail.item.MailItem;
import io.xeres.common.protocol.xrs.RsServiceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static io.xeres.app.xrs.service.RsServiceKeys.KEY_PARTIAL_MESSAGE_LIST;
import static io.xeres.common.protocol.xrs.RsServiceType.MESSAGES;

@Component
public class MailRsService extends RsService
{
	private static final Logger log = LoggerFactory.getLogger(MailRsService.class);

	private final MailService mailService;

	protected MailRsService(RsServiceRegistry rsServiceRegistry, MailService mailService)
	{
		super(rsServiceRegistry);
		this.mailService = mailService;
	}

	@Override
	public RsServiceType getServiceType()
	{
		return MESSAGES;
	}

	@Transactional
	@Override
	public void handleItem(PeerConnection sender, Item item)
	{
		if (item instanceof MailItem mailItem)
		{
			handleMailItem(sender, mailItem);
		}
	}

	private void handleMailItem(PeerConnection sender, MailItem item)
	{
		if (item.isPartial())
		{
			handlePartialMail(sender, item);
		}
		else
		{
			handleMail(sender, item);
		}
	}

	private void handleMail(PeerConnection sender, MailItem item)
	{
		var message = item.getMessage();
		if (message.length() > RsServiceConstants.MESSAGE_SPLIT_SLICE_SIZE_MAX)
		{
			log.error("Mail from {} exceed split message size", sender);
			return;
		}
		var messageList = sender.getServiceData(this, KEY_PARTIAL_MESSAGE_LIST);
		if (messageList.isPresent())
		{
			@SuppressWarnings("unchecked")
			var existingList = (List<String>) messageList.get();
			existingList.add(message);
			message = String.join("", existingList);
			sender.removeServiceData(this, KEY_PARTIAL_MESSAGE_LIST);
		}

		mailService.receiveDirectMail(
				item.getSent(),
				sender.getLocation().getLocationIdentifier(),
				item.getSubject(),
				message,
				new HashSet<>(item.getLocationsTo()),
				new HashSet<>(item.getLocationsCc()),
				item.getAttachments()
		);
	}

	private void handlePartialMail(PeerConnection sender, MailItem item)
	{
		if (item.getMessage().length() > RsServiceConstants.MESSAGE_SPLIT_SLICE_SIZE_MAX)
		{
			log.error("Mail from {} exceed split message size", sender);
			return;
		}
		var messageList = sender.getServiceData(this, KEY_PARTIAL_MESSAGE_LIST);
		if (messageList.isEmpty())
		{
			List<String> newMessageList = new ArrayList<>();
			newMessageList.add(item.getMessage());
			sender.putServiceData(this, KEY_PARTIAL_MESSAGE_LIST, newMessageList);
		}
		else
		{
			//noinspection unchecked
			((List<String>) messageList.get()).add(item.getMessage());
		}
	}
}
