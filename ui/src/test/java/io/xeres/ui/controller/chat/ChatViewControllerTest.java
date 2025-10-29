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

package io.xeres.ui.controller.chat;

import io.xeres.common.i18n.I18nUtils;
import io.xeres.common.message.chat.ChatRoomContext;
import io.xeres.common.message.chat.ChatRoomInfo;
import io.xeres.common.message.chat.ChatRoomLists;
import io.xeres.testutils.IdFakes;
import io.xeres.ui.client.*;
import io.xeres.ui.client.message.MessageClient;
import io.xeres.ui.custom.asyncimage.ImageCache;
import io.xeres.ui.model.location.Location;
import io.xeres.ui.model.profile.Profile;
import io.xeres.ui.support.markdown.MarkdownService;
import io.xeres.ui.support.preference.PreferenceUtils;
import io.xeres.ui.support.sound.SoundPlayerService;
import io.xeres.ui.support.tray.TrayService;
import io.xeres.ui.support.uri.UriService;
import io.xeres.ui.support.window.WindowManager;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.testfx.framework.junit5.ApplicationExtension;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith({ApplicationExtension.class, MockitoExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatViewControllerTest
{
	@Mock
	private MessageClient messageClient;

	@Mock
	private ChatClient chatClient;

	@Mock
	private ProfileClient profileClient;

	@Mock
	private LocationClient locationClient;

	@Mock
	private WindowManager windowManager;

	@Mock
	private TrayService trayService;

	@Spy
	private final ResourceBundle resourceBundle = I18nUtils.getBundle();

	@Mock
	private MarkdownService markdownService;

	@Mock
	private UriService uriService;

	@Mock
	private GeneralClient generalClient;

	@Mock
	private ImageCache imageCache;

	@Mock
	private SoundPlayerService soundPlayerService;

	@Mock
	private ShareClient shareClient;

	@InjectMocks
	private ChatViewController controller;

	@Test
	void testFxmlLoading() throws IOException
	{
		FXMLLoader loader = new FXMLLoader(ChatViewControllerTest.class.getResource("/view/chat/chat_view.fxml"), resourceBundle);

		loader.setControllerFactory(_ -> controller);

		var ownProfile = new Profile();
		ownProfile.setName("foobar");

		var location = new Location();
		location.setName("Foobar location");
		location.setLocationIdentifier(IdFakes.createLocationIdentifier());

		PreferenceUtils.setLocation(location);

		var chatRoomList = new ChatRoomLists();
		chatRoomList.addAvailable(new ChatRoomInfo("availableRoom"));
		chatRoomList.addSubscribed(new ChatRoomInfo("subscribedRoom"));
		var chatRoomUser = new io.xeres.common.message.chat.ChatRoomUser("foobar", null, 1L);
		var chatRoomContext = new ChatRoomContext(chatRoomList, chatRoomUser);

		when(profileClient.getOwn()).thenReturn(Mono.just(ownProfile));
		when(chatClient.getChatRoomContext()).thenReturn(Mono.just(chatRoomContext));

		Parent root = loader.load();

		assertNotNull(root);
	}
}