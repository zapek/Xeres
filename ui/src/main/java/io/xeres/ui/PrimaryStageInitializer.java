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

package io.xeres.ui;

import io.xeres.common.properties.StartupProperties;
import io.xeres.ui.client.ProfileClient;
import io.xeres.ui.client.message.ChatFrameHandler;
import io.xeres.ui.client.message.MessageClient;
import io.xeres.ui.controller.chat.ChatViewController;
import io.xeres.ui.support.window.WindowManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Hooks;

import static io.xeres.common.properties.StartupProperties.Property.ICONIFIED;
import static io.xeres.common.rest.PathConfig.CHAT_PATH;

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

		profileClient.getOwn()
				.doOnSuccess(profile -> windowManager.openMain(event.getStage(), profile, StartupProperties.getBoolean(ICONIFIED, false)))
				.doOnError(WebClientResponseException.class, e -> {
					if (e.getStatusCode() == HttpStatus.NOT_FOUND)
					{
						windowManager.openAccountCreation(event.getStage());
					}
				}) // XXX: try finding out what happens if ie. the host is unreachable (connecting to unknown host or so). we probably need some other doOnError()
				.subscribe();

		messageClient.subscribe(CHAT_PATH, new ChatFrameHandler(windowManager, chatViewController));
		messageClient.connect(); // XXX: not so nice here. what about future subscription systems?
	}
}
