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

package io.xeres.ui.controller.messaging;

import io.xeres.common.i18n.I18nUtils;
import io.xeres.testutils.IdFakes;
import io.xeres.ui.client.*;
import io.xeres.ui.client.message.MessageClient;
import io.xeres.ui.custom.asyncimage.ImageCache;
import io.xeres.ui.support.markdown.MarkdownService;
import io.xeres.ui.support.uri.UriService;
import io.xeres.ui.support.window.WindowManager;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testfx.framework.junit5.ApplicationExtension;

import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith({ApplicationExtension.class, MockitoExtension.class})
class MessagingWindowControllerTest
{
	@Mock
	private ProfileClient profileClient;

	@Mock
	private IdentityClient identityClient;

	@Mock
	private MarkdownService markdownService;

	@Mock
	private WindowManager windowManager;

	@Mock
	private UriService uriService;

	@Spy
	private final ResourceBundle resourceBundle = I18nUtils.getBundle();

	@Mock
	private MessageClient messageClient;

	@Mock
	private ShareClient shareClient;

	@Mock
	private ChatClient chatClient;

	@Mock
	private GeneralClient generalClient;

	@Mock
	private LocationClient locationClient;

	@Mock
	private ImageCache imageCache;

	private AutoCloseable closeable;

	@BeforeEach
	void setUp()
	{
		closeable = MockitoAnnotations.openMocks(this);
	}

	@AfterEach
	void tearDown() throws Exception
	{
		closeable.close();
	}

	@Test
	void testFxmlLoading() throws Exception
	{
		FXMLLoader loader = new FXMLLoader(MessagingWindowControllerTest.class.getResource("/view/messaging/messaging.fxml"), resourceBundle);

		var controller = new MessagingWindowController(profileClient, identityClient, windowManager, uriService, messageClient, shareClient, markdownService, IdFakes.createLocationIdentifier(), resourceBundle, chatClient, generalClient, imageCache, locationClient, false);

		loader.setControllerFactory(_ -> controller);

		Parent root = loader.load();

		assertNotNull(root);
	}
}