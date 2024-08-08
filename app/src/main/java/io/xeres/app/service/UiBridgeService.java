/*
 * Copyright (c) 2024 by David Gerber - https://zapek.com
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

package io.xeres.app.service;

import io.xeres.common.tray.TrayNotificationType;
import io.xeres.ui.client.message.MessageClient;
import io.xeres.ui.support.splash.SplashService;
import io.xeres.ui.support.tray.TrayService;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * This class allows to call methods in the UI module. This (and XeresApplication) should be the only classes being able to do that.
 * This helps separate concerns as long as this class stays as small as possible.
 * There's an ArchUnit rule that finds violations.
 */
@Service
public class UiBridgeService
{
	private final SplashService splashService;
	private final TrayService trayService;
	private final WebClient.Builder webClientBuilder;
	private final MessageClient messageClient;

	public UiBridgeService(SplashService splashService, TrayService trayService, WebClient.Builder webClientBuilder, MessageClient messageClient)
	{
		this.splashService = splashService;
		this.trayService = trayService;
		this.webClientBuilder = webClientBuilder;
		this.messageClient = messageClient;
	}

	public void setSplashStatus(String description)
	{
		splashService.status(description);
	}

	public void closeSplashScreen()
	{
		splashService.close();
	}

	public void showTrayNotification(TrayNotificationType type, String message)
	{
		trayService.showNotification(type, message);
	}

	public void setTrayStatus(String message)
	{
		trayService.setTooltip(message);
	}

	public void setClientsAuthentication(String username, String password)
	{
		webClientBuilder.defaultHeaders(httpHeaders -> httpHeaders.setBasicAuth(username, password));
		messageClient.setAuthentication(username, password);
	}
}
