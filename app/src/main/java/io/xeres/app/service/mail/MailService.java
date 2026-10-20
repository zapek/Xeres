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

package io.xeres.app.service.mail;

import io.xeres.app.database.model.mail.MailDestination;
import io.xeres.app.database.model.mail.MailMessage;
import io.xeres.app.database.model.mail.MailMessageSummary;
import io.xeres.app.database.repository.MailDestinationRepository;
import io.xeres.app.database.repository.MailMessageRepository;
import io.xeres.app.xrs.common.FileSet;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.LocationIdentifier;
import io.xeres.common.mail.MailType;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class MailService
{
	private final MailMessageRepository mailMessageRepository;
	private final MailDestinationRepository mailDestinationRepository;

	public MailService(MailMessageRepository mailMessageRepository, MailDestinationRepository mailDestinationRepository)
	{
		this.mailMessageRepository = mailMessageRepository;
		this.mailDestinationRepository = mailDestinationRepository;
	}

	public void receiveDirectMail(Instant sent, LocationIdentifier from, String subject, String message, Set<LocationIdentifier> to, Set<LocationIdentifier> cc, FileSet attachments)
	{
		var mailMessage = new MailMessage(MailType.INBOX, sent, from, null, subject, message, to, Set.of(), attachments);
		cc.forEach(mailMessage::addCc);
		mailMessageRepository.save(mailMessage);
	}

	public Page<MailMessageSummary> findAllMailMessagesSummary(MailType mailType, Pageable pageable)
	{
		return mailMessageRepository.findSummaryAllByType(mailType, pageable);
	}

	public Window<MailMessageSummary> findAllMailMessagesSummary(MailType mailType, ScrollPosition position, Sort sort, Limit limit)
	{
		return mailMessageRepository.findSummaryAllByType(mailType, position, sort, limit);
	}

	public Optional<MailMessage> findMessageById(long id)
	{
		return mailMessageRepository.findById(id);
	}

	@Transactional
	public long createMailMessage(LocationIdentifier locationFrom, GxsId identityFrom, Set<LocationIdentifier> locationsTo, Set<LocationIdentifier> locationsCc, Set<LocationIdentifier> locationsBcc, Set<GxsId> identitiesTo, Set<GxsId> identitiesCc, Set<GxsId> identitiesBcc, String subject, String content, FileSet files)
	{
		// XXX: send notifications, etc...
		var mail = new MailMessage(MailType.OUTBOX, null, locationFrom, identityFrom, subject, content, locationsTo, identitiesTo, files);
		locationsCc.forEach(mail::addCc);
		locationsBcc.forEach(mail::addBcc);
		identitiesCc.forEach(mail::addCc);
		identitiesBcc.forEach(mail::addBcc);
		var mailMessage = mailMessageRepository.save(mail);

		// Create the destinations
		List<LocationIdentifier> locations = new ArrayList<>();
		locations.addAll(mailMessage.getLocationsTo());
		locations.addAll(mailMessage.getLocationsCc());
		locations.addAll(mailMessage.getLocationsBcc());

		List<GxsId> identities = new ArrayList<>();
		identities.addAll(mailMessage.getIdentitiesTo());
		identities.addAll(mailMessage.getIdentitiesCc());
		identities.addAll(mailMessage.getIdentitiesBcc());

		locations.forEach(location -> mailDestinationRepository.save(new MailDestination(mailMessage, location)));
		identities.forEach(identity -> mailDestinationRepository.save(new MailDestination(mailMessage, identity)));

		return mailMessage.getId();
	}
}
