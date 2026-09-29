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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

import javax.tools.ToolProvider;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.junit.jupiter.api.Assertions.*;

class PluginLoaderTest
{
	@TempDir
	Path temporary;

	@Test
	void missingDirectoryLoadsNoPlugins() throws Exception
	{
		try (var plugins = PluginLoader.load(temporary.resolve("absent"), getClass().getClassLoader()))
		{
			assertTrue(plugins.plugins().isEmpty());
		}
	}

	@Test
	void discoversExternalJarAndSharesHostApi() throws Exception
	{
		var directory = temporary.resolve("plugins");
		createPlugin(directory, "Example", "example", XeresPlugin.API_VERSION, XeresPlugin.API_VERSION);
		try (var plugins = PluginLoader.load(directory, getClass().getClassLoader()))
		{
			assertEquals(1, plugins.plugins().size());
			var plugin = plugins.plugins().getFirst();
			assertEquals("example", plugin.id());
			assertSame(plugins.classLoader(), plugin.getClass().getClassLoader());
			assertEquals("example.Example$PluginBean", plugin.components().getFirst().getName());
		}
	}

	@Test
	void initializerRegistersBeansBeforeRefresh() throws Exception
	{
		var directory = temporary.resolve("plugins");
		createPlugin(directory, "Example", "example", XeresPlugin.API_VERSION, XeresPlugin.API_VERSION);
		var originalLoader = Thread.currentThread().getContextClassLoader();
		try (var context = new AnnotationConfigApplicationContext())
		{
			context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", Map.of("xeres.plugins.directory", directory.toString())));
			new PluginInitializer().initialize(context);
			context.refresh();
			assertTrue(context.getEnvironment().getProperty("xeres.plugins.example.enabled", Boolean.class, false));
			assertNull(context.getEnvironment().getProperty("xeres.plugins.absent.enabled"));
			var type = context.getClassLoader().loadClass("example.Example$PluginBean");
			assertNotNull(context.getBean(type));
		}
		finally
		{
			Thread.currentThread().setContextClassLoader(originalLoader);
		}
	}

	@Test
	void rejectsIncompatibleManifestBeforeLoadingClasses() throws Exception
	{
		var directory = temporary.resolve("plugins");
		createPlugin(directory, "Example", "example", 99, XeresPlugin.API_VERSION);
		var error = assertThrows(IllegalStateException.class, () -> PluginLoader.load(directory, getClass().getClassLoader()));
		assertTrue(error.getCause().getMessage().contains("Incompatible plugin API"));
	}

	@Test
	void rejectsIncompatibleProvider() throws Exception
	{
		var directory = temporary.resolve("plugins");
		createPlugin(directory, "Example", "example", XeresPlugin.API_VERSION, 99);
		assertThrows(IllegalStateException.class, () -> PluginLoader.load(directory, getClass().getClassLoader()));
	}

	@Test
	void rejectsDuplicateIds() throws Exception
	{
		var directory = temporary.resolve("plugins");
		createPlugin(directory, "First", "example", XeresPlugin.API_VERSION, XeresPlugin.API_VERSION);
		createPlugin(directory, "Second", "example", XeresPlugin.API_VERSION, XeresPlugin.API_VERSION);
		var error = assertThrows(IllegalStateException.class, () -> PluginLoader.load(directory, getClass().getClassLoader()));
		assertTrue(error.getCause().getMessage().contains("Duplicate plugin ID"));
	}

	@Test
	void rejectsTwoVersionsOfSameProvider() throws Exception
	{
		var directory = temporary.resolve("plugins");
		createPlugin(directory, "Example", "example", XeresPlugin.API_VERSION, XeresPlugin.API_VERSION);
		Files.copy(directory.resolve("Example.jar"), directory.resolve("Example-old.jar"));
		var error = assertThrows(IllegalStateException.class, () -> PluginLoader.load(directory, getClass().getClassLoader()));
		assertTrue(error.getCause().getMessage().contains("Duplicate plugin provider"));
	}

	@Test
	void rejectsCorruptJar() throws Exception
	{
		var directory = Files.createDirectory(temporary.resolve("plugins"));
		Files.writeString(directory.resolve("broken.jar"), "not a jar");
		assertThrows(IllegalStateException.class, () -> PluginLoader.load(directory, getClass().getClassLoader()));
	}

	private void createPlugin(Path directory, String name, String id, int manifestVersion, int providerVersion) throws Exception
	{
		Files.createDirectories(directory);
		var classes = Files.createDirectory(temporary.resolve(name));
		var source = classes.resolve(name + ".java");
		Files.writeString(source, """
				package example;
				public class %s implements io.xeres.plugin.XeresPlugin {
				    public String id() { return "%s"; }
				    public int apiVersion() { return %d; }
				    public java.util.List<Class<?>> components() { return java.util.List.of(PluginBean.class); }
				    public static class PluginBean { }
				}
				""".formatted(name, id, providerVersion));
		var api = Path.of(XeresPlugin.class.getProtectionDomain().getCodeSource().getLocation().toURI());
		assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null,
				"-classpath", api.toString(), "-d", classes.toString(), source.toString()));
		var manifest = new Manifest();
		manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
		manifest.getMainAttributes().putValue("Xeres-Plugin-Api", Integer.toString(manifestVersion));
		try (var jar = new JarOutputStream(Files.newOutputStream(directory.resolve(name + ".jar")), manifest);
			 var files = Files.walk(classes))
		{
			for (var file : files.filter(path -> path.toString().endsWith(".class")).toList())
			{
				jar.putNextEntry(new JarEntry(classes.relativize(file).toString().replace('\\', '/')));
				Files.copy(file, jar);
				jar.closeEntry();
			}
			jar.putNextEntry(new JarEntry("META-INF/services/" + XeresPlugin.class.getName()));
			jar.write(("example." + name).getBytes(StandardCharsets.UTF_8));
			jar.closeEntry();
		}
	}
}
