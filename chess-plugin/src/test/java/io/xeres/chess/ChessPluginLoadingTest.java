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

package io.xeres.chess;

import io.xeres.app.application.plugin.PluginInitializer;
import io.xeres.app.service.IdentityService;
import io.xeres.app.service.MessageService;
import io.xeres.app.xrs.service.RsServiceRegistry;
import io.xeres.common.protocol.xrs.RsServiceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ChessPluginLoadingTest extends FXTest
{
	@TempDir
	Path directory;

	@org.junit.jupiter.params.ParameterizedTest
	@org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
	void externalChessJarRegistersControllersAndNetworkService(boolean gui) throws Exception
	{
		Files.copy(Path.of(System.getProperty("xeres.test.chessJar")), directory.resolve("chess.jar"));
		var originalLoader = Thread.currentThread().getContextClassLoader();
		// Spring's resource scanner otherwise caches a JarFile beyond context.close() on Windows.
		// Production plugins are replaced only after the application process exits.
		var cacheJars = java.net.URLConnection.getDefaultUseCaches("jar");
		java.net.URLConnection.setDefaultUseCaches("jar", false);
		// Hide Gradle's chess classes so the test must load the actual distributed JAR.
		var host = new ClassLoader(originalLoader)
		{
			@Override
			public java.net.URL getResource(String name)
			{
				return name.contains("chess") ? null : super.getResource(name);
			}

			@Override
			protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException
			{
				if (name.startsWith("io.xeres.chess."))
				{
					throw new ClassNotFoundException(name);
				}
				return super.loadClass(name, resolve);
			}
		};
		try (var context = new AnnotationConfigApplicationContext())
		{
			context.setClassLoader(host);
			context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", Map.of(
					"xeres.plugins.directory", directory.toString(), "xrs.ui.enabled", gui)));
			new PluginInitializer().initialize(context);
			context.registerBean("rsServiceRegistry", RsServiceRegistry.class, () -> new RsServiceRegistry(context.getEnvironment()));
			context.registerBean(IdentityService.class, () -> mock(IdentityService.class));
			context.registerBean(MessageService.class, () -> mock(MessageService.class));
			context.registerBean(ObjectMapper.class, () -> new JsonMapper());
			if (gui)
			{
				context.registerBean(java.util.ResourceBundle.class, () -> java.util.ResourceBundle.getBundle("i18n.messages", java.util.Locale.ENGLISH));
				context.registerBean(org.springframework.web.reactive.function.client.WebClient.Builder.class, org.springframework.web.reactive.function.client.WebClient::builder);
				for (var dependency : java.util.List.of(io.xeres.ui.client.GeneralClient.class, io.xeres.ui.client.IdentityClient.class,
						io.xeres.ui.client.ContactClient.class, io.xeres.ui.custom.asyncimage.ImageCache.class,
						io.xeres.ui.support.own.OwnCache.class, io.xeres.ui.support.window.WindowManager.class))
				{
					context.getBeanFactory().registerSingleton(dependency.getSimpleName(), mock(dependency));
				}
			}
			context.refresh();
			var service = context.getBean(RsServiceRegistry.class).getServiceFromType(ChessServiceType.CHESS.getType());
			assertNotNull(service);
			assertTrue(service.isRunning());
			assertSame(context.getClassLoader(), service.getClass().getClassLoader());
			assertEquals(gui, context.containsBean("chessUiPlugin"));
			if (gui)
			{
				var extension = context.getBean(io.xeres.ui.plugin.UiPlugin.class);
				assertEquals("chess", extension.tabs().getFirst().id());
				assertEquals(1, extension.settingsPages().size());
				assertEquals("chess.invitations", extension.notificationSettings().getFirst().id());
				assertEquals(1, extension.identityActions().size());
				var checked = new java.util.concurrent.CompletableFuture<Void>();
				javafx.application.Platform.runLater(() -> {
					var previous = Thread.currentThread().getContextClassLoader();
					try
					{
						Thread.currentThread().setContextClassLoader(context.getClassLoader());
						var settings = extension.settingsPages().getFirst();
						var resource = settings.controller().getResource("/view/settings/settings_chess.fxml");
						assertNotNull(resource);
						assertTrue(resource.toString().startsWith("jar:"));
						assertTrue(resource.toString().contains("chess.jar!"));
						var weaver = new net.rgielen.fxweaver.core.FxWeaver(context::getBean, () -> {});
						assertNotNull(weaver.loadView(settings.controller(), settings.resources()));
						checked.complete(null);
					}
					catch (Throwable failure)
					{
						checked.completeExceptionally(failure);
					}
					finally
					{
						Thread.currentThread().setContextClassLoader(previous);
					}
				});
				checked.get(30, java.util.concurrent.TimeUnit.SECONDS);
			}
			assertTrue(context.containsBean("chessController"));
			assertTrue(context.containsBean("chessMessageController"));
			assertTrue(context.getEnvironment().getProperty("xeres.plugins.chess.enabled", Boolean.class, false));
		}
		finally
		{
			Thread.currentThread().setContextClassLoader(originalLoader);
			java.net.URLConnection.setDefaultUseCaches("jar", cacheJars);
		}
	}
}
