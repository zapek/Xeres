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
import io.xeres.app.service.notification.mail.MailNotificationService;
import io.xeres.app.xrs.common.FileSet;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.Identifier;
import io.xeres.common.id.LocationIdentifier;
import io.xeres.common.mail.DeliveryType;
import io.xeres.common.mail.MailType;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

@Service
public class MailService
{
	private final MailMessageRepository mailMessageRepository;
	private final MailNotificationService mailNotificationService;
	private final MailDestinationRepository mailDestinationRepository;

	public MailService(MailMessageRepository mailMessageRepository, MailNotificationService mailNotificationService, MailDestinationRepository mailDestinationRepository)
	{
		this.mailMessageRepository = mailMessageRepository;
		this.mailNotificationService = mailNotificationService;
		this.mailDestinationRepository = mailDestinationRepository;
	}

	public void saveIncomingMailMessage(Instant sent, Identifier from, String subject, String message, Set<LocationIdentifier> locationsTo, Set<GxsId> identitiesTo, @Nullable Set<LocationIdentifier> locationsCc, @Nullable Set<GxsId> identitiesCc, FileSet attachments)
	{
		var mailMessage = new MailMessage(MailType.INBOX,
				sent,
				from instanceof LocationIdentifier locationFrom ? locationFrom : null,
				from instanceof GxsId identityFrom ? identityFrom : null,
				subject,
				message,
				attachments);
		emptyIfNull(locationsTo).forEach(mailMessage::addTo);
		emptyIfNull(identitiesTo).forEach(mailMessage::addTo);
		emptyIfNull(locationsCc).forEach(mailMessage::addCc);
		emptyIfNull(identitiesCc).forEach(mailMessage::addCc);
		var saved = mailMessageRepository.save(mailMessage);
		mailNotificationService.addMailMessages(List.of(saved));
	}

	public Page<MailMessageSummary> findAllMailMessagesSummary(MailType mailType, Pageable pageable)
	{
		return mailMessageRepository.findSummaryAllByType(mailType, pageable);
	}

	public Window<MailMessageSummary> findAllMailMessagesSummary(MailType mailType, ScrollPosition position, Sort sort, Limit limit)
	{
		return mailMessageRepository.findSummaryAllByType(mailType, position, sort, limit);
	}

	public Optional<MailMessage> findMailMessageById(long id)
	{
		return mailMessageRepository.findById(id);
	}

	@Transactional
	public long createOutgoingMailMessage(LocationIdentifier locationFrom, GxsId identityFrom, Set<LocationIdentifier> locationsTo, Set<LocationIdentifier> locationsCc, Set<LocationIdentifier> locationsBcc, Set<GxsId> identitiesTo, Set<GxsId> identitiesCc, Set<GxsId> identitiesBcc, String subject, String content, FileSet files)
	{
		var mailMessage = new MailMessage(MailType.OUTBOX, null, locationFrom, identityFrom, subject, content, files);
		emptyIfNull(locationsTo).forEach(mailMessage::queueTo);
		emptyIfNull(locationsCc).forEach(mailMessage::queueCc);
		emptyIfNull(locationsBcc).forEach(mailMessage::queueBcc);
		emptyIfNull(identitiesTo).forEach(mailMessage::queueTo);
		emptyIfNull(identitiesCc).forEach(mailMessage::queueCc);
		emptyIfNull(identitiesBcc).forEach(mailMessage::queueBcc);
		var saved = mailMessageRepository.save(mailMessage);

		mailNotificationService.addMailMessages(List.of(saved));

		return saved.getId();
	}

	public List<MailDestination> getPendingOutgoingMessages(LocationIdentifier locationIdentifier)
	{
		return mailDestinationRepository.findAllByTypeAndDestination(DeliveryType.LOCATION, locationIdentifier.getBytes());
	}

	public List<MailDestination> getPendingOutgoingMessages(Set<Identifier> identifiers)
	{
		return mailDestinationRepository.findAllByDestinationIn(identifiers.stream().map(Identifier::getBytes).toList());
	}

	public List<MailDestination> getPendingOutgoingMessages()
	{
		return mailDestinationRepository.findAll();
	}
}
