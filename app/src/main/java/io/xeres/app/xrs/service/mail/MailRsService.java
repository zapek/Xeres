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

import io.xeres.app.database.DatabaseSession;
import io.xeres.app.database.DatabaseSessionManager;
import io.xeres.app.database.model.mail.MailDestination;
import io.xeres.app.net.peer.PeerConnection;
import io.xeres.app.service.mail.MailService;
import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.service.RsService;
import io.xeres.app.xrs.service.RsServiceConstants;
import io.xeres.app.xrs.service.RsServiceInitPriority;
import io.xeres.app.xrs.service.RsServiceRegistry;
import io.xeres.app.xrs.service.mail.item.MailItem;
import io.xeres.common.id.Identifier;
import io.xeres.common.mail.DeliveryType;
import io.xeres.common.protocol.xrs.RsServiceType;
import io.xeres.common.util.ThreadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static io.xeres.app.xrs.service.RsServiceKeys.KEY_PARTIAL_MESSAGE_LIST;
import static io.xeres.common.protocol.xrs.RsServiceType.MESSAGES;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

@Component
public class MailRsService extends RsService
{
	private static final Logger log = LoggerFactory.getLogger(MailRsService.class);

	private Thread mailSenderManagerThread;
	private final BlockingQueue<MailSenderAction> mailSenderQueue = new LinkedBlockingQueue<>();

	private final MailService mailService;
	private final DatabaseSessionManager databaseSessionManager;
	private final OutgoingMailSender outgoingMailSender;

	protected MailRsService(RsServiceRegistry rsServiceRegistry, MailService mailService, DatabaseSessionManager databaseSessionManager, OutgoingMailSender outgoingMailSender)
	{
		super(rsServiceRegistry);
		this.mailService = mailService;
		this.databaseSessionManager = databaseSessionManager;
		this.outgoingMailSender = outgoingMailSender;
	}

	@Override
	public RsServiceType getServiceType()
	{
		return MESSAGES;
	}

	@Override
	public RsServiceInitPriority getInitPriority()
	{
		return RsServiceInitPriority.NORMAL;
	}

	@Override
	public void initialize()
	{
		var mailSenderManager = new MailSenderManager(this, mailSenderQueue);
		mailSenderManagerThread = Thread.ofVirtual()
				.name("Mail Sender Manager")
				.start(mailSenderManager);
	}

	@Override
	public void initialize(PeerConnection peerConnection)
	{
		mailSenderQueue.add(new MailSenderActionCheckPeer(peerConnection));
	}

	@Override
	public void shutdown()
	{
		ThreadUtils.interruptAndWait(mailSenderManagerThread);
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

	public void sendMailToDestinations(Set<Identifier> destinations)
	{
		mailSenderQueue.add(new MailSenderActionCheckDestinations(destinations));
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

		mailService.saveIncomingMailMessage(
				item.getSent(),
				sender.getLocation().getLocationIdentifier(),
				item.getSubject(),
				message,
				new HashSet<>(emptyIfNull(item.getLocationsTo())),
				null,
				new HashSet<>(emptyIfNull(item.getLocationsCc())),
				null,
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

	void sendMail(PeerConnection peerConnection)
	{
		try (var ignored = new DatabaseSession(databaseSessionManager))
		{
			mailService.getPendingOutgoingMessages(peerConnection.getLocation().getLocationIdentifier()).forEach(mailDestination -> outgoingMailSender.send(peerConnection, mailDestination, this));
		}
	}

	void sendMails(Set<Identifier> destinations)
	{
		try (var ignored = new DatabaseSession(databaseSessionManager))
		{
			mailService.getPendingOutgoingMessages(destinations).forEach(this::sendMailToDestination);
		}
	}

	void sendMails()
	{
		try (var ignored = new DatabaseSession(databaseSessionManager))
		{
			mailService.getPendingOutgoingMessages().forEach(this::sendMailToDestination);
		}
	}

	private void sendMailToDestination(MailDestination mailDestination)
	{
		if (mailDestination.getType() == DeliveryType.LOCATION)
		{
			outgoingMailSender.send(mailDestination, this);
		}
		else if (mailDestination.getType() == DeliveryType.DISTANT)
		{
			// XXX: needs to handle distant here
		}
	}
}
