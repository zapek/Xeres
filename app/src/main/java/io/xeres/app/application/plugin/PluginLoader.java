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

import io.xeres.plugin.XeresPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.ServiceLoader;
import java.util.jar.JarFile;

/// One parent-first class loader keeps host API types shared across all plugins.
/// Invalid plugin installations fail startup with an actionable error.
public final class PluginLoader implements AutoCloseable
{
	private static final Logger log = LoggerFactory.getLogger(PluginLoader.class);
	private final URLClassLoader classLoader;
	private final List<XeresPlugin> plugins;

	private PluginLoader(URLClassLoader classLoader, List<XeresPlugin> plugins)
	{
		this.classLoader = classLoader;
		this.plugins = List.copyOf(plugins);
	}

	public static PluginLoader load(Path directory, ClassLoader parent)
	{
		URLClassLoader loader = null;
		try
		{
			var urls = new ArrayList<URL>();
			var providerNames = new HashSet<String>();
			if (Files.exists(directory))
			{
				try (var files = Files.list(directory))
				{
					for (var path : files.filter(Files::isRegularFile)
							.filter(file -> file.toString().toLowerCase(Locale.ROOT).endsWith(".jar")).sorted().toList())
					{
						try (var jar = new JarFile(path.toFile()))
						{
							var manifest = jar.getManifest();
							var version = manifest == null ? null : manifest.getMainAttributes().getValue("Xeres-Plugin-Api");
							if (!Integer.toString(XeresPlugin.API_VERSION).equals(version))
							{
								throw new IllegalArgumentException("Incompatible plugin API in " + path + ": " + version);
							}
							var descriptor = jar.getJarEntry("META-INF/services/" + XeresPlugin.class.getName());
							if (descriptor == null)
							{
								throw new IllegalArgumentException("Missing plugin provider in " + path);
							}
							try (var stream = jar.getInputStream(descriptor))
							{
								var names = new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).lines()
										.map(line -> line.split("#", 2)[0].trim()).filter(line -> !line.isEmpty()).toList();
								if (names.isEmpty())
								{
									throw new IllegalArgumentException("Empty plugin provider in " + path);
								}
								for (var name : names)
								{
									if (!providerNames.add(name))
									{
										throw new IllegalArgumentException("Duplicate plugin provider " + name + " in " + path);
									}
								}
							}
						}
						urls.add(path.toUri().toURL());
					}
				}
			}
			loader = new URLClassLoader(urls.toArray(URL[]::new), parent);
			var plugins = new ArrayList<XeresPlugin>();
			var ids = new HashSet<String>();
			for (var provider : ServiceLoader.load(XeresPlugin.class, loader).stream().toList())
			{
				// Ignore providers already on the host classpath; only external JARs are plugins.
				if (provider.type().getClassLoader() != loader)
				{
					continue;
				}
				var plugin = provider.get();
				if (plugin.apiVersion() != XeresPlugin.API_VERSION || plugin.id() == null || !plugin.id().matches("[a-z][a-z0-9-]*"))
				{
					throw new IllegalArgumentException("Invalid plugin descriptor: " + plugin.getClass().getName());
				}
				if (!ids.add(plugin.id()))
				{
					throw new IllegalArgumentException("Duplicate plugin ID: " + plugin.id());
				}
				List.copyOf(plugin.components()); // Validate component descriptors before changing the host context.
				plugins.add(plugin);
				log.info("Loaded plugin {} from {}", plugin.id(), directory);
			}
			if (plugins.size() != providerNames.size())
			{
				throw new IllegalArgumentException("Plugin providers must be defined in external JARs, not shadowed by host classes");
			}
			return new PluginLoader(loader, plugins);
		}
		catch (IOException | RuntimeException | java.util.ServiceConfigurationError | LinkageError e)
		{
			if (loader != null)
			{
				try
				{
					loader.close();
				}
				catch (IOException closeFailure)
				{
					e.addSuppressed(closeFailure);
				}
			}
			throw new IllegalStateException("Cannot load Xeres plugins from " + directory + ". Remove or replace the incompatible JAR and restart.", e);
		}
	}

	public ClassLoader classLoader()
	{
		return classLoader;
	}

	public List<XeresPlugin> plugins()
	{
		return plugins;
	}

	@Override
	public void close() throws IOException
	{
		if (Thread.currentThread().getContextClassLoader() == classLoader)
		{
			Thread.currentThread().setContextClassLoader(classLoader.getParent());
		}
		classLoader.close();
	}
}
