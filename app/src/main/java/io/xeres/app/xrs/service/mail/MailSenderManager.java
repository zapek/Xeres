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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

class MailSenderManager implements Runnable
{
	private static final Logger log = LoggerFactory.getLogger(MailSenderManager.class);

	private final MailRsService mailRsService;
	private final BlockingQueue<MailSenderAction> queue;

	public MailSenderManager(MailRsService mailRsService, BlockingQueue<MailSenderAction> queue)
	{
		this.mailRsService = mailRsService;
		this.queue = queue;
	}

	@Override
	public void run()
	{
		var done = false;

		while (!done)
		{
			try
			{
				var action = getNextAction();
				processAction(action);
			}
			catch (InterruptedException _)
			{
				log.debug("MailSenderManager thread interrupted");
				cleanup();
				done = true;
				Thread.currentThread().interrupt();
			}
			catch (RuntimeException e)
			{
				log.error("Exception in MailSenderManager", e);
			}
		}
	}

	private void cleanup()
	{

	}

	private MailSenderAction getNextAction() throws InterruptedException
	{
		return queue.poll(5, TimeUnit.MINUTES); // XXX: is that time slot ok?
	}

	private void processAction(MailSenderAction action)
	{
		switch (action)
		{
			case MailSenderActionCheckPeer mailSenderActionCheckPeer -> mailRsService.sendMail(mailSenderActionCheckPeer.peerConnection());
			case MailSenderActionCheckDestinations mailSenderActionCheckDestinations -> mailRsService.sendMails(mailSenderActionCheckDestinations.destinations());
			case null -> mailRsService.sendMails();
		}
	}
}
