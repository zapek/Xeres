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
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.xeres.common.rest.notification.availability.AvailabilityChange;
import io.xeres.common.rest.notification.board.AddOrUpdateBoardGroups;
import io.xeres.common.rest.notification.board.AddOrUpdateBoardMessages;
import io.xeres.common.rest.notification.board.SetBoardGroupMessagesReadState;
import io.xeres.common.rest.notification.board.SetBoardMessagesReadState;
import io.xeres.common.rest.notification.channel.AddOrUpdateChannelGroups;
import io.xeres.common.rest.notification.channel.AddOrUpdateChannelMessages;
import io.xeres.common.rest.notification.channel.SetChannelGroupMessagesReadState;
import io.xeres.common.rest.notification.channel.SetChannelMessagesReadState;
import io.xeres.common.rest.notification.contact.AddOrUpdateContacts;
import io.xeres.common.rest.notification.contact.RemoveContacts;
import io.xeres.common.rest.notification.file.FileNotification;
import io.xeres.common.rest.notification.file.FileSearchNotification;
import io.xeres.common.rest.notification.file.FileTrendNotification;
import io.xeres.common.rest.notification.forum.AddOrUpdateForumGroups;
import io.xeres.common.rest.notification.forum.AddOrUpdateForumMessages;
import io.xeres.common.rest.notification.forum.SetForumGroupMessagesReadState;
import io.xeres.common.rest.notification.forum.SetForumMessagesReadState;
import io.xeres.common.rest.notification.status.StatusNotification;

import static io.xeres.common.rest.notification.Notification.*;

/**
 * Notification superclass. It's important to list all of its subclasses in it because the "type" field is used
 * by Jackson to know which subclass to deserialize from. Changing the strings names should be avoided as this could
 * break the API if there's a 3rd party client.
 */
@JsonTypeInfo(
		use = JsonTypeInfo.Id.NAME,
		include = JsonTypeInfo.As.PROPERTY,
		property = "type"
)
@JsonSubTypes({
		// Boards
		@JsonSubTypes.Type(value = AddOrUpdateBoardGroups.class, name = ADD_OR_UPDATE_BOARD_GROUPS),
		@JsonSubTypes.Type(value = AddOrUpdateBoardMessages.class, name = ADD_OR_UPDATE_BOARD_MESSAGES),
		@JsonSubTypes.Type(value = SetBoardGroupMessagesReadState.class, name = SET_BOARD_GROUP_MESSAGES_READ_STATE),
		@JsonSubTypes.Type(value = SetBoardMessagesReadState.class, name = SET_BOARD_MESSAGES_READ_STATE),
		// Channels
		@JsonSubTypes.Type(value = AddOrUpdateChannelGroups.class, name = ADD_OR_UPDATE_CHANNEL_GROUPS),
		@JsonSubTypes.Type(value = AddOrUpdateChannelMessages.class, name = ADD_OR_UPDATE_CHANNEL_MESSAGES),
		@JsonSubTypes.Type(value = SetChannelGroupMessagesReadState.class, name = SET_CHANNEL_GROUP_MESSAGES_READ_STATE),
		@JsonSubTypes.Type(value = SetChannelMessagesReadState.class, name = SET_CHANNEL_MESSAGES_READ_STATE),
		// Forums
		@JsonSubTypes.Type(value = AddOrUpdateForumGroups.class, name = ADD_OR_UPDATE_FORUM_GROUPS),
		@JsonSubTypes.Type(value = AddOrUpdateForumMessages.class, name = ADD_OR_UPDATE_FORUM_MESSAGES),
		@JsonSubTypes.Type(value = SetForumGroupMessagesReadState.class, name = SET_FORUM_GROUP_MESSAGES_READ_STATE),
		@JsonSubTypes.Type(value = SetForumMessagesReadState.class, name = SET_FORUM_MESSAGES_READ_STATE),
		// Availability
		@JsonSubTypes.Type(value = AvailabilityChange.class, name = AVAILABILITY_CHANGE),
		// Contact
		@JsonSubTypes.Type(value = AddOrUpdateContacts.class, name = ADD_OR_UPDATE_CONTACTS),
		@JsonSubTypes.Type(value = RemoveContacts.class, name = REMOVE_CONTACTS),
		// File
		@JsonSubTypes.Type(value = FileNotification.class, name = FILE),
		@JsonSubTypes.Type(value = FileSearchNotification.class, name = FILE_SEARCH),
		@JsonSubTypes.Type(value = FileTrendNotification.class, name = FILE_TREND),
		// Status
		@JsonSubTypes.Type(value = StatusNotification.class, name = STATUS),
})
public interface Notification
{
	String ADD_OR_UPDATE_BOARD_GROUPS = "add_or_update_board_groups";
	String ADD_OR_UPDATE_BOARD_MESSAGES = "add_or_update_board_messages";
	String SET_BOARD_GROUP_MESSAGES_READ_STATE = "set_board_group_messages_read_state";
	String SET_BOARD_MESSAGES_READ_STATE = "set_board_messages_read_state";

	String ADD_OR_UPDATE_CHANNEL_GROUPS = "add_or_update_channel_groups";
	String ADD_OR_UPDATE_CHANNEL_MESSAGES = "add_or_update_channel_messages";
	String SET_CHANNEL_GROUP_MESSAGES_READ_STATE = "set_channel_group_messages_read_state";
	String SET_CHANNEL_MESSAGES_READ_STATE = "set_channel_messages_read_state";

	String ADD_OR_UPDATE_FORUM_GROUPS = "add_or_update_forum_groups";
	String ADD_OR_UPDATE_FORUM_MESSAGES = "add_or_update_forum_messages";
	String SET_FORUM_GROUP_MESSAGES_READ_STATE = "set_forum_group_messages_read_state";
	String SET_FORUM_MESSAGES_READ_STATE = "set_forum_messages_read_state";

	String AVAILABILITY_CHANGE = "availability_change";

	String ADD_OR_UPDATE_CONTACTS = "add_or_update_contacts";
	String REMOVE_CONTACTS = "remove_contacts";

	String FILE = "file";
	String FILE_SEARCH = "file_search";
	String FILE_TREND = "file_trend";
	String STATUS = "status";

	String getType();

	default boolean ignoreDuplicates()
	{
		return false;
	}
}
