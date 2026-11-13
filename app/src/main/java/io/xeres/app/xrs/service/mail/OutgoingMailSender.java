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

import io.xeres.app.database.model.mail.MailDestination;
import io.xeres.app.database.model.mail.MailMessage;
import io.xeres.app.database.repository.MailDestinationRepository;
import io.xeres.app.net.peer.PeerConnection;
import io.xeres.app.net.peer.PeerConnectionManager;
import io.xeres.app.service.location.LocationService;
import io.xeres.app.xrs.common.FileSet;
import io.xeres.app.xrs.service.RsService;
import io.xeres.app.xrs.service.mail.item.MailItem;
import io.xeres.common.id.LocationIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class OutgoingMailSender
{
	private static final Logger log = LoggerFactory.getLogger(OutgoingMailSender.class);

	private final MailDestinationRepository mailDestinationRepository;
	private final PeerConnectionManager peerConnectionManager;
	private final LocationService locationService;

	OutgoingMailSender(MailDestinationRepository mailDestinationRepository, PeerConnectionManager peerConnectionManager, LocationService locationService)
	{
		this.mailDestinationRepository = mailDestinationRepository;
		this.peerConnectionManager = peerConnectionManager;
		this.locationService = locationService;
	}

	@Transactional
	public void send(PeerConnection peerConnection, MailDestination mailDestination, RsService rsService)
	{
		log.debug("Trying to send mail to {}", peerConnection);
		var mailItem = buildMailItem(mailDestination.getMailMessage());
		var status = peerConnectionManager.writeItem(peerConnection, mailItem, rsService);
		if (status.isDone() && !status.isSuccess())
		{
			log.error("Can't send mail to {}: {}", peerConnection.getLocation(), status.cause().getMessage());
			return;
		}
		mailDestinationRepository.delete(mailDestination);
	}

	@Transactional
	public void send(MailDestination mailDestination, RsService rsService)
	{
		locationService.findLocationByLocationIdentifier(new LocationIdentifier(mailDestination.getDestination())).ifPresent(location -> {
			var mailItem = buildMailItem(mailDestination.getMailMessage());
			var status = peerConnectionManager.writeItem(location, mailItem, rsService);
			if (status.isDone() && !status.isSuccess())
			{
				log.debug("Can't send mail to {} but it's not connected: {}", location, status.cause().getMessage());
				return;
			}
			mailDestinationRepository.delete(mailDestination);
		});
	}

	private static MailItem buildMailItem(MailMessage mailMessage)
	{
		var tos = Stream.of(mailMessage.getLocationsTo(), mailMessage.getIdentitiesTo())
				.flatMap(Collection::stream)
				.collect(Collectors.toSet());
		var ccs = Stream.of(mailMessage.getLocationsCc(), mailMessage.getIdentitiesCc())
				.flatMap(Collection::stream)
				.collect(Collectors.toSet());
		return new MailItem(tos, ccs, mailMessage.getSubject(), mailMessage.getContent(), new FileSet(mailMessage.getFiles(), null, null));
	}
}
