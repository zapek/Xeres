/*
 * Copyright (c) 2023-2026 by David Gerber - https://zapek.com
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

package io.xeres.common.rest.notification;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import io.xeres.common.rest.notification.availability.AvailabilityChange;
import io.xeres.common.rest.notification.board.AddOrUpdateBoardGroups;
import io.xeres.common.rest.notification.board.AddOrUpdateBoardMessages;
import io.xeres.common.rest.notification.board.SetBoardGroupMessagesReadState;
import io.xeres.common.rest.notification.board.SetBoardMessageReadState;
import io.xeres.common.rest.notification.channel.AddOrUpdateChannelGroups;
import io.xeres.common.rest.notification.channel.AddOrUpdateChannelMessages;
import io.xeres.common.rest.notification.channel.SetChannelGroupMessagesReadState;
import io.xeres.common.rest.notification.channel.SetChannelMessageReadState;
import io.xeres.common.rest.notification.contact.AddOrUpdateContacts;
import io.xeres.common.rest.notification.contact.RemoveContacts;
import io.xeres.common.rest.notification.file.FileNotification;
import io.xeres.common.rest.notification.file.FileSearchNotification;
import io.xeres.common.rest.notification.file.FileTrendNotification;
import io.xeres.common.rest.notification.forum.AddOrUpdateForumGroups;
import io.xeres.common.rest.notification.forum.AddOrUpdateForumMessages;
import io.xeres.common.rest.notification.forum.SetForumGroupMessagesReadState;
import io.xeres.common.rest.notification.forum.SetForumMessageReadState;
import io.xeres.common.rest.notification.status.StatusNotification;

/**
 * Notification superclass. It's important to list all of its subclasses in it because the "type" field is used
 * by Jackson to know which subclass to deserialize from. Changing the type names should be avoided as this could
 * break the API if there's a 3rd party client.
 */
@JsonTypeInfo(
		use = Id.NAME,
		include = As.PROPERTY,
		property = "type"
)
@JsonSubTypes({
		// Boards
		@Type(value = AddOrUpdateBoardGroups.class, name = "add_or_update_board_groups"),
		@Type(value = AddOrUpdateBoardMessages.class, name = "add_or_update_board_messages"),
		@Type(value = SetBoardGroupMessagesReadState.class, name = "set_board_group_messages_read_state"),
		@Type(value = SetBoardMessageReadState.class, name = "set_board_message_read_state"),
		// Channels
		@Type(value = AddOrUpdateChannelGroups.class, name = "add_or_update_channel_groups"),
		@Type(value = AddOrUpdateChannelMessages.class, name = "add_or_update_channel_messages"),
		@Type(value = SetChannelGroupMessagesReadState.class, name = "set_channel_group_messages_read_state"),
		@Type(value = SetChannelMessageReadState.class, name = "set_channel_message_read_state"),
		// Forums
		@Type(value = AddOrUpdateForumGroups.class, name = "add_or_update_forum_groups"),
		@Type(value = AddOrUpdateForumMessages.class, name = "add_or_update_forum_messages"),
		@Type(value = SetForumGroupMessagesReadState.class, name = "set_forum_group_messages_read_state"),
		@Type(value = SetForumMessageReadState.class, name = "set_forum_message_read_state"),
		// Availability
		@Type(value = AvailabilityChange.class, name = "availability_change"),
		// Contact
		@Type(value = AddOrUpdateContacts.class, name = "add_or_update_contacts"),
		@Type(value = RemoveContacts.class, name = "remove_contacts"),
		// File
		@Type(value = FileNotification.class, name = "file"),
		@Type(value = FileSearchNotification.class, name = "file_search"),
		@Type(value = FileTrendNotification.class, name = "file_trend"),
		// Status
		@Type(value = StatusNotification.class, name = "status"),
})
public interface Notification
{
	default boolean ignoreDuplicates()
	{
		return false;
	}
}
