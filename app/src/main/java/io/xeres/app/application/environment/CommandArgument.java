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

package io.xeres.app.application.environment;

import io.xeres.common.AppName;
import io.xeres.common.mui.MinimalUserInterface;
import io.xeres.common.properties.StartupProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Utility class to handle user supplied command line arguments.
 */
public final class CommandArgument
{
	private CommandArgument()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	private static final String HELP = "help";
	private static final String VERSION = "version";
	private static final String NO_GUI = "no-gui";
	private static final String DATA_DIR = "data-dir";
	private static final String CONTROL_PORT = "control-port";
	private static final String SERVER_PORT = "server-port";
	private static final String FAST_SHUTDOWN = "fast-shutdown";
	public static final String SERVER_ONLY = "server-only";
	public static final String REMOTE_CONNECT = "remote-connect";
	public static final String ICONIFIED = "iconified";

	/**
	 * Parses command line arguments. Should be called before Spring Boot is initialized.
	 *
	 * @param args the command line arguments
	 */
	public static void parse(String[] args)
	{
		ApplicationArguments appArgs = new DefaultApplicationArguments(args);

		for (var arg : appArgs.getNonOptionArgs())
		{
			switch (arg)
			{
				case "-h", "-help", "help" -> showHelp();
				default -> throw new IllegalArgumentException("Unknown argument [" + arg + "]. Run with the --help argument.");
			}
		}

		for (var arg : appArgs.getOptionNames())
		{
			switch (arg)
			{
				case HELP -> showHelp();
				case VERSION -> showVersion();
				case DATA_DIR -> setString(StartupProperties.Property.DATA_DIR, appArgs, arg);
				case CONTROL_PORT -> {
					setPort(StartupProperties.Property.CONTROL_PORT, appArgs, arg);
					setPort(StartupProperties.Property.UI_PORT, appArgs, arg);
				}
				case SERVER_PORT -> setPort(StartupProperties.Property.SERVER_PORT, appArgs, arg);
				case REMOTE_CONNECT -> {
					var ipAndPort = appArgs.getOptionValues(arg).stream()
							.findFirst()
							.orElseThrow(() -> new IllegalArgumentException(REMOTE_CONNECT + " must specify a host or host:port like 'localhost' or 'localhost:1066'"));
					StartupProperties.setUiRemoteConnect(ipAndPort);
				}
				case NO_GUI -> setBooleanInverted(StartupProperties.Property.UI, appArgs, arg);
				case FAST_SHUTDOWN -> setBoolean(StartupProperties.Property.FAST_SHUTDOWN, appArgs, arg);
				case SERVER_ONLY -> setBoolean(StartupProperties.Property.SERVER_ONLY, appArgs, arg);
				case ICONIFIED -> setBoolean(StartupProperties.Property.ICONIFIED, appArgs, arg);
				default -> throw new IllegalArgumentException("Unknown argument " + arg);
			}
		}
	}

	private static void setBoolean(StartupProperties.Property property, ApplicationArguments appArgs, String arg)
	{
		if (!appArgs.getOptionValues(arg).isEmpty())
		{
			throw new IllegalArgumentException("--" + arg + " doesn't expect a value");
		}
		StartupProperties.setBoolean(property, "true");
	}

	private static void setBooleanInverted(StartupProperties.Property property, ApplicationArguments appArgs, String arg)
	{
		if (!appArgs.getOptionValues(arg).isEmpty())
		{
			throw new IllegalArgumentException("--" + arg + " doesn't expect a value");
		}
		StartupProperties.setBoolean(property, "false");
	}

	private static void setString(StartupProperties.Property property, ApplicationArguments appArgs, String arg)
	{
		try
		{
			StartupProperties.setString(property, getValue(appArgs, arg));
		}
		catch (IllegalArgumentException e)
		{
			throw new IllegalArgumentException("--" + arg + " does not contain a value");
		}
	}

	private static void setPort(StartupProperties.Property property, ApplicationArguments appArgs, String arg)
	{
		try
		{
			StartupProperties.setPort(property, getValue(appArgs, arg));
		}
		catch (IllegalArgumentException e)
		{
			throw new IllegalArgumentException("--" + arg + " must specify a port bigger than 0 and smaller than 65536");
		}
	}

	private static String getValue(ApplicationArguments appArgs, String arg)
	{
		var optionValues = appArgs.getOptionValues(arg);
		if (optionValues.isEmpty())
		{
			throw new IllegalArgumentException("--" + arg + " expects a value");
		}
		else if (optionValues.size() > 1)
		{
			throw new IllegalArgumentException("--" + arg + " cannot be specified more than once");
		}
		return optionValues.get(0);
	}

	private static void showHelp()
	{
		var output = String.format("""
				Usage: %s [--options]
				where options include:
				   --no-gui                            start without an UI
				   --iconified                         start iconified into the tray
				   --data-dir=<path>                   specify the data directory
				   --control-port=<port>               specify the control port for remote access
				   --server-port=<port>                specify the local port to bind to for incoming peer connections
				   --fast-shutdown                     ignore proper shutdown procedure (not recommended)
				   --server-only                       only accept incoming connections, do not make outgoing ones
				   --remote-connect=<host>[:<port>]    act as an UI client only and connect to a remote server
				   --version                           print the version of the software
				   --help                              print this help message
				See https://xeres.io/docs/ for more details.
				""", AppName.NAME);

		portableOutput(output);
		System.exit(0);
	}

	private static void showVersion()
	{
		var buildInfo = CommandArgument.class.getClassLoader().getResourceAsStream("META-INF/build-info.properties");
		if (buildInfo != null)
		{
			var reader = new BufferedReader(new InputStreamReader(buildInfo));
			reader.lines().filter(s -> s.startsWith("build.version="))
					.forEach(s -> portableOutput(AppName.NAME + " " + s.substring(s.indexOf('=') + 1)));
		}
		System.exit(0);
	}

	private static void portableOutput(String s)
	{
		if (System.console() != null)
		{
			System.out.print(s);
		}
		else
		{
			MinimalUserInterface.showInformation(s);
		}
	}
}
