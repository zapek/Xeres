/*
 * Copyright (c) 2019-2021 by David Gerber - https://zapek.com
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

package io.xeres.app.xrs.service.chat;

import io.xeres.app.database.model.location.LocationFakes;
import io.xeres.app.net.peer.PeerConnection;
import io.xeres.app.net.peer.PeerConnectionManager;
import io.xeres.app.xrs.service.chat.item.ChatMessageItem;
import io.xeres.common.message.MessageType;
import io.xeres.common.message.chat.PrivateChatMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.EnumSet;

import static io.xeres.common.rest.PathConfig.CHAT_PATH;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(SpringExtension.class)
class ChatRsServiceTest
{
	@Mock
	private PeerConnectionManager peerConnectionManager;

	@InjectMocks
	private ChatRsService chatRsService;

	// Unfortunately, only simple stuff can be tested. The rest requires mocking a lot of stuff (identities, keys, etc...)

	@Test
	void ChatService_HandleChatMessageItem_OK()
	{
		var MESSAGE = "hello";
		var peerConnection = new PeerConnection(LocationFakes.createLocation(), null);

		var item = new ChatMessageItem(MESSAGE, EnumSet.of(ChatFlags.PRIVATE));
		chatRsService.handleItem(peerConnection, item);

		verify(peerConnectionManager).sendToClientSubscriptions(eq(CHAT_PATH), eq(MessageType.CHAT_PRIVATE_MESSAGE), eq(peerConnection.getLocation().getLocationId()), any(PrivateChatMessage.class));
	}
}
