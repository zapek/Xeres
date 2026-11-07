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

package io.xeres.app.service.notification.mail;

import io.xeres.app.database.model.mail.MailMessage;
import io.xeres.app.service.mail.MailMessageService;
import io.xeres.app.service.notification.NotificationService;
import io.xeres.app.service.unhtml.UnHtmlService;
import io.xeres.common.rest.notification.mail.AddMailMessages;
import org.springframework.stereotype.Service;

import java.util.List;

import static io.xeres.app.database.model.mail.MailMapper.toMailMessageDTOs;

@Service
public class MailNotificationService extends NotificationService
{
	private final MailMessageService mailMessageService;
	private final UnHtmlService unHtmlService;

	public MailNotificationService(MailMessageService mailMessageService, UnHtmlService unHtmlService)
	{
		super();
		this.mailMessageService = mailMessageService;
		this.unHtmlService = unHtmlService;
	}

	public void addMailMessages(List<MailMessage> messages)
	{
		sendNotification(new AddMailMessages(toMailMessageDTOs(unHtmlService, messages,
				mailMessageService.getLocationsMapFromMessages(messages),
				mailMessageService.getIdentitiesMapFromMessages(messages),
				false)));
	}

	public void removeMailMessages(List<MailMessage> messages)
	{
		// XXX
	}
}
