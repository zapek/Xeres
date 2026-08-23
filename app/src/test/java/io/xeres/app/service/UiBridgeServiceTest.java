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

package io.xeres.app.service;

import io.xeres.common.tray.TrayNotificationType;
import io.xeres.ui.client.message.MessageClient;
import io.xeres.ui.support.splash.SplashService;
import io.xeres.ui.support.tray.TrayService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Base64;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UiBridgeServiceTest
{
	@Mock
	private SplashService splashService;

	@Mock
	private TrayService trayService;

	@Mock
	private WebClient.Builder webClientBuilder;

	@Mock
	private MessageClient messageClient;

	@Captor
	private ArgumentCaptor<Consumer<HttpHeaders>> httpHeadersConsumer;

	@InjectMocks
	private UiBridgeService uiBridgeService;

	@Test
	void setSplashStatus_Database()
	{
		uiBridgeService.setSplashStatus(UiBridgeService.SplashStatus.DATABASE);

		verify(splashService).status(SplashService.Status.DATABASE);
	}

	@Test
	void setSplashStatus_Network()
	{
		uiBridgeService.setSplashStatus(UiBridgeService.SplashStatus.NETWORK);

		verify(splashService).status(SplashService.Status.NETWORK);
	}

	@Test
	void closeSplashScreen_Success()
	{
		uiBridgeService.closeSplashScreen();

		verify(splashService).close();
	}

	@Test
	void showTrayNotification_Success()
	{
		uiBridgeService.showTrayNotification(TrayNotificationType.CONNECTION, "connected");

		verify(trayService).showNotification(TrayNotificationType.CONNECTION, "connected");
	}

	@Test
	void setTrayStatus_Success()
	{
		uiBridgeService.setTrayStatus("status message");

		verify(trayService).setTooltip("status message");
	}

	@Test
	void setClientsAuthentication_Success()
	{
		var httpHeaders = new HttpHeaders();

		uiBridgeService.setClientsAuthentication("user", "password");

		verify(webClientBuilder).defaultHeaders(httpHeadersConsumer.capture());
		httpHeadersConsumer.getValue().accept(httpHeaders);

		var expected = "Basic " + Base64.getEncoder().encodeToString("user:password".getBytes());
		assertEquals(expected, httpHeaders.getFirst(HttpHeaders.AUTHORIZATION));

		verify(messageClient).setAuthentication("user", "password");
	}
}
