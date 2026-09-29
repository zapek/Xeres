/*
 * Copyright (c) 2019-2026 by David Gerber - https://zapek.com
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

package io.xeres.ui.plugin;

import io.xeres.ui.PrimaryStageInitializer;
import io.xeres.ui.client.message.MessageClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PluginUiServiceTest
{
	@Test
	void hostRunsWithoutAnyPluginContributions()
	{
		var factory = new DefaultListableBeanFactory();
		var plugins = new PluginUiService(factory.getBeanProvider(UiPlugin.class));
		assertTrue(plugins.tabs().isEmpty());
		assertTrue(plugins.settingsPages().isEmpty());
		assertTrue(plugins.identityActions().isEmpty());
		var messages = mock(MessageClient.class);
		plugins.subscribe(messages);
		plugins.onExit();
		verifyNoInteractions(messages);
	}

	@Test
	void subscriptionsAreAddedBeforeConnectingAndExitIsForwarded()
	{
		var factory = new DefaultListableBeanFactory();
		var plugin = mock(UiPlugin.class);
		factory.registerSingleton("testPlugin", plugin);
		var plugins = new PluginUiService(factory.getBeanProvider(UiPlugin.class));
		var messages = mock(MessageClient.class);
		when(messages.subscribe(anyString(), any())).thenReturn(messages);
		new PrimaryStageInitializer(null, null, null, messages, null, plugins).onNetworkReadyEvent(null);
		var order = inOrder(plugin, messages);
		order.verify(plugin).subscribe(messages);
		order.verify(messages).connect();
		plugins.onExit();
		verify(plugin).onExit();
	}
}
