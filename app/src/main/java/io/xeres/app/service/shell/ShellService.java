/*
 * Copyright (c) 2024-2025 by David Gerber - https://zapek.com
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

package io.xeres.app.service.shell;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import io.xeres.common.mui.MinimalUserInterface;
import io.xeres.common.mui.Shell;
import io.xeres.common.mui.ShellResult;
import io.xeres.common.util.ByteUnitUtils;
import jakarta.annotation.PreDestroy;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.stereotype.Service;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static io.xeres.common.mui.ShellAction.*;

@Service
public class ShellService implements Shell
{
	private final History history = new History(20);

	@Override
	public ShellResult sendCommand(String input)
	{
		var args = new DefaultApplicationArguments(translateCommandline(input));
		var arg = args.getNonOptionArgs().isEmpty() ? null : args.getNonOptionArgs().getFirst();

		if (StringUtils.isAsciiPrintable(arg))
		{
			history.addCommand(input);
			return switch (arg.toLowerCase(Locale.ROOT))
			{
				case "help", "?" -> new ShellResult(SUCCESS, """
						Available commands:
						  - help: displays this help
						  - avail: shows the available memory
						  - clear: clears the screen
						  - cpu: shows the CPU count
						  - exit: closes the shell
						  - gc: runs the garbage collector
						  - loglevel [package] [level]: sets the log level of a package
						  - properties: opens the properties window
						  - pwd: shows the current directory
						  - uname: shows the operating system
						  - uptime: shows the app uptime""");
				case "exit", "endshell", "endcli" -> new ShellResult(EXIT);
				case "clear", "cls" -> new ShellResult(CLS);
				case "avail", "free" -> getMemorySpecs();
				case "cpu" -> getCpuCount();
				case "pwd", "cd" -> getWorkingDirectory();
				case "properties", "props" -> getProperties();
				case "uname" -> getOperatingSystem();
				case "uptime" -> getUptime();
				case "gc" -> runGc();
				case "loglevel" -> setLogLevel(args.getNonOptionArgs().size() > 1 ? args.getNonOptionArgs().get(1) : null, args.getNonOptionArgs().size() > 2 ? args.getNonOptionArgs().get(2) : null);
				case "loadwb" -> new ShellResult(SUCCESS, "Not again!");
				default -> new ShellResult(UNKNOWN_COMMAND, arg);
			};
		}
		return new ShellResult(NO_OP);
	}

	@Override
	public String getPreviousCommand()
	{
		return history.getPrevious();
	}

	@Override
	public String getNextCommand()
	{
		return history.getNext();
	}

	private ShellResult getProperties()
	{
		var properties = System.getProperties();

		var map = properties.entrySet().stream()
				.collect(Collectors.toMap(k -> (String) k.getKey(), e -> (String) e.getValue()))
				.entrySet().stream()
				.sorted(Map.Entry.comparingByKey())
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
						(oldValue, newValue) -> oldValue, LinkedHashMap::new));

		var sb = new StringBuilder();
		map.forEach((key, value) -> processProperty(sb, key, value));

		return new ShellResult(SUCCESS, sb.toString());
	}

	private static void processProperty(StringBuilder sb, String key, String value)
	{
		if (key.endsWith(".path"))
		{
			value = String.join("\n", value.split(File.pathSeparator));
		}
		else
		{
			value = showLineSeparator(value);
		}
		sb.append(key).append(" = ").append(value).append("\n");
	}

	private static String showLineSeparator(String in)
	{
		in = in.replace("\n", "\\n");
		in = in.replace("\r", "\\r");
		return in;
	}

	private static ShellResult getMemorySpecs()
	{
		var totalMemory = Runtime.getRuntime().totalMemory();
		return new ShellResult(SUCCESS,
				"Memory allocated for the JVM: " + ByteUnitUtils.fromBytes(totalMemory) + "\n" +
						"Used memory: " + ByteUnitUtils.fromBytes(totalMemory - Runtime.getRuntime().freeMemory()) + "\n" +
						"Maximum allocatable memory: " + ByteUnitUtils.fromBytes(Runtime.getRuntime().maxMemory()));
	}

	private static ShellResult getUptime()
	{
		var startTime = ManagementFactory.getRuntimeMXBean().getStartTime();
		var currentTime = System.currentTimeMillis();
		var uptimeMillis = currentTime - startTime;

		var duration = Duration.ofMillis(uptimeMillis);

		var days = duration.toDays();
		var hours = duration.toHours() % 24;
		var minutes = duration.toMinutes() % 60;
		var seconds = duration.getSeconds() % 60;

		return new ShellResult(SUCCESS,
				String.format("%d days, %d hours, %d minutes, %d seconds",
						days, hours, minutes, seconds));
	}

	private static ShellResult getCpuCount()
	{
		return new ShellResult(SUCCESS,
				"CPU count: " + Runtime.getRuntime().availableProcessors());
	}

	private static ShellResult getWorkingDirectory()
	{
		return new ShellResult(SUCCESS, System.getProperty("user.dir"));
	}

	private static ShellResult getOperatingSystem()
	{
		return new ShellResult(SUCCESS, System.getProperty("os.name") + " (" + System.getProperty("os.arch") + ")");
	}

	private static ShellResult runGc()
	{
		System.gc();
		return new ShellResult(SUCCESS, "Done");
	}

	private static ShellResult setLogLevel(String packageName, String level)
	{
		if (StringUtils.isBlank(packageName))
		{
			return new ShellResult(SUCCESS, "Package name must be provided (eg. io.xeres.app.application.Startup)");
		}
		if (StringUtils.isBlank(level))
		{
			return new ShellResult(SUCCESS, "Log level must be provided (trace, debug, info, warn, error)");
		}

		var loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		var logger = loggerContext.exists(packageName);
		if (logger == null)
		{
			return new ShellResult(SUCCESS, "No such logger");
		}

		logger.setLevel(Level.valueOf(level));

		return new ShellResult(SUCCESS, "Level of " + logger.getName() + " changed to " + logger.getLevel());
	}

	/**
	 * [code borrowed from ant.jar]
	 * Crack a command line.
	 *
	 * @param toProcess the command line to process.
	 * @return the command line broken into strings.
	 * An empty or null toProcess parameter results in a zero sized array.
	 */
	static String[] translateCommandline(String toProcess)
	{
		enum State
		{
			NORMAL,
			IN_QUOTE,
			IN_DOUBLE_QUOTE
		}

		if (StringUtils.isEmpty(toProcess))
		{
			//no command? no string
			return new String[0];
		}
		// parse with a simple finite state machine

		var state = State.NORMAL;
		final var tok = new StringTokenizer(toProcess, "\"' ", true);
		final var current = new StringBuilder();
		final ArrayList<String> result = new ArrayList<>();
		var lastTokenHasBeenQuoted = false;

		while (tok.hasMoreTokens())
		{
			String nextTok = tok.nextToken();
			switch (state)
			{
				case State.IN_QUOTE ->
				{
					if ("'".equals(nextTok))
					{
						lastTokenHasBeenQuoted = true;
						state = State.NORMAL;
					}
					else
					{
						current.append(nextTok);
					}
				}
				case State.IN_DOUBLE_QUOTE ->
				{
					if ("\"".equals(nextTok))
					{
						lastTokenHasBeenQuoted = true;
						state = State.NORMAL;
					}
					else
					{
						current.append(nextTok);
					}
				}
				default ->
				{
					switch (nextTok)
					{
						case "'" -> state = State.IN_QUOTE;
						case "\"" -> state = State.IN_DOUBLE_QUOTE;
						case " " ->
						{
							if (lastTokenHasBeenQuoted || !current.isEmpty())
							{
								result.add(current.toString());
								current.setLength(0);
							}
						}
						case null, default -> current.append(nextTok);
					}
					lastTokenHasBeenQuoted = false;
				}
			}
		}
		if (lastTokenHasBeenQuoted || !current.isEmpty())
		{
			result.add(current.toString());
		}
		if (state == State.IN_QUOTE || state == State.IN_DOUBLE_QUOTE)
		{
			throw new RuntimeException("unbalanced quotes in " + toProcess);
		}
		return result.toArray(new String[0]);
	}

	@PreDestroy
	private void cleanup()
	{
		MinimalUserInterface.closeShell();
	}
}
