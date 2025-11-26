/*
 * Copyright (c) 2025 by David Gerber - https://zapek.com
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

package io.xeres.app.service.notification.channel;

import io.xeres.app.service.notification.NotificationService;
import io.xeres.app.xrs.service.channel.item.ChannelGroupItem;
import io.xeres.app.xrs.service.channel.item.ChannelMessageItem;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChannelNotificationService extends NotificationService
{

	public void addOrUpdateChannelGroups(List<ChannelGroupItem> ChannelGroups)
	{
		// XXX
	}

	public void addChannelMessages(List<ChannelMessageItem> ChannelMessages)
	{
		// XXX
	}

	public void markChannelMessagesAsRead(List<ChannelMessageItem> ChannelMessages)
	{
		// XXX
	}
}
