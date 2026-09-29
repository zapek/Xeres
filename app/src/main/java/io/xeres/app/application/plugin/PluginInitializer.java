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

package io.xeres.app.application.plugin;

import io.xeres.app.application.environment.DataDirLocator;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.core.env.MapPropertySource;

import java.nio.file.Path;
import java.util.HashMap;

/// Loads optional plugins before component scanning, REST mapping and service startup.
public class PluginInitializer implements ApplicationContextInitializer<GenericApplicationContext>
{
	@Override
	public void initialize(GenericApplicationContext context)
	{
		var dataDir = DataDirLocator.getDataDir();
		var defaultDirectory = Path.of(dataDir == null ? "data" : dataDir, "plugins").toString();
		var directory = Path.of(context.getEnvironment().getProperty("xeres.plugins.directory", defaultDirectory));
		var plugins = PluginLoader.load(directory, context.getClassLoader());
		var properties = new HashMap<String, Object>();
		plugins.plugins().forEach(plugin -> properties.put("xeres.plugins." + plugin.id() + ".enabled",
				context.getEnvironment().getProperty("xrs.service." + plugin.id() + ".enabled", Boolean.class, true)));
		context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("loadedPlugins", properties));
		context.setClassLoader(plugins.classLoader());
		Thread.currentThread().setContextClassLoader(plugins.classLoader());
		context.getBeanFactory().setBeanClassLoader(plugins.classLoader());
		var reader = new AnnotatedBeanDefinitionReader(context, context.getEnvironment());
		plugins.plugins().forEach(plugin -> {
			reader.register(plugin.components().toArray(Class<?>[]::new));
			if (context.getEnvironment().getProperty("xrs.ui.enabled", Boolean.class, true))
			{
				reader.register(plugin.uiComponents().toArray(Class<?>[]::new));
			}
		});
		context.getBeanFactory().registerSingleton("xeresPlugins", plugins);
		context.getDefaultListableBeanFactory().registerDisposableBean("xeresPlugins", plugins::close);
	}
}
