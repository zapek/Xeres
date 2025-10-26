/*
 * Copyright (c) 2019-2025 by David Gerber - https://zapek.com
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

package io.xeres.ui;

import io.xeres.common.events.ConnectWebSocketsEvent;
import io.xeres.common.properties.StartupProperties;
import io.xeres.ui.client.ProfileClient;
import io.xeres.ui.client.message.*;
import io.xeres.ui.controller.chat.ChatViewController;
import io.xeres.ui.event.StageReadyEvent;
import io.xeres.ui.support.util.UiUtils;
import io.xeres.ui.support.window.WindowManager;
import javafx.application.Platform;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Hooks;

import static io.xeres.common.message.MessagePath.*;
import static io.xeres.common.properties.StartupProperties.Property.ICONIFIED;
import static io.xeres.common.properties.StartupProperties.Property.UI;

@Component
public class PrimaryStageInitializer
{
	private static final Logger log = LoggerFactory.getLogger(PrimaryStageInitializer.class);

	private final WindowManager windowManager;
	private final ChatViewController chatViewController;
	private final ProfileClient profileClient;
	private final MessageClient messageClient;

	public PrimaryStageInitializer(WindowManager windowManager, ChatViewController chatViewController, ProfileClient profileClient, MessageClient messageClient)
	{
		this.windowManager = windowManager;
		this.chatViewController = chatViewController;
		this.profileClient = profileClient;
		this.messageClient = messageClient;
	}

	@EventListener
	public void onApplicationEvent(StageReadyEvent event)
	{
		Hooks.onErrorDropped(throwable -> log.debug("WebClient warning: {}", throwable.getMessage())); // Suppress Reactor's error messages

		// Do not exit the platform when all windows are closed.
		Platform.setImplicitExit(false);

		profileClient.getOwn()
				.doFirst(() -> Platform.runLater(() -> {
					if (SystemUtils.IS_OS_MAC)
					{
						// This is needed because of https://bugs.openjdk.org/browse/JDK-8248127
						// "AppKit Thread" has a null class loader which prevents resources from being loaded so
						// we have to set it. The AppKit Thread seems to be related with AWT, so as soon as either
						// a splash screen or a systray is being used, it will be there.
						if (Thread.currentThread().getContextClassLoader() == null)
						{
							Thread.currentThread().setContextClassLoader(PrimaryStageInitializer.class.getClassLoader());
						}
					}
					windowManager.calculateWindowDecorationSizes(event.getStage());
				}))
				.doOnSuccess(profile -> windowManager.openMain(event.getStage(), profile, StartupProperties.getBoolean(ICONIFIED, false)))
				.doOnError(WebClientResponseException.class, e -> {
					if (e.getStatusCode() == HttpStatus.NOT_FOUND)
					{
						windowManager.openAccountCreation(event.getStage());
					}
				})
				.doOnError(WebClientRequestException.class, e -> UiUtils.showAlertError(e, Platform::exit))
				.subscribe();
	}

	@EventListener
	public void onNetworkReadyEvent(ConnectWebSocketsEvent unused)
	{
		if (!StartupProperties.getBoolean(UI, true))
		{
			return;
		}

		if (messageClient.isConnected())
		{
			return;
		}

		messageClient
				.subscribe(chatPrivateDestination(), new PrivateChatFrameHandler(windowManager))
				.subscribe(chatRoomDestination(), new ChatRoomFrameHandler(chatViewController))
				.subscribe(chatDistantDestination(), new DistantChatFrameHandler(windowManager))
				.subscribe(chatBroadcastDestination(), new BroadcastChatFrameHandler())
				.subscribe(voipPrivateDestination(), new VoipFrameHandler(windowManager))
				.connect();
	}
}
