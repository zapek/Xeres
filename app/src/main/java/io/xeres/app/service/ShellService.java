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

import io.xeres.common.mui.MinimalUserInterface;
import io.xeres.common.mui.Shell;
import io.xeres.common.mui.ShellResult;
import jakarta.annotation.PreDestroy;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Locale;
import java.util.StringTokenizer;

import static io.xeres.common.mui.ShellAction.*;

@Service
public class ShellService implements Shell
{
	@Override
	public ShellResult sendCommand(String input)
	{
		var args = new DefaultApplicationArguments(translateCommandline(input));
		var arg = args.getNonOptionArgs().getFirst();

		if (arg != null)
		{
			return switch (arg.toLowerCase(Locale.ROOT))
			{
				case "help", "?" -> new ShellResult(SUCCESS, """
						Available commands:
						  - help: displays this help
						  - clear: clears the screen
						  - avail: shows the available memory
						  - pwd: shows the current directory
						  - uname: shows the operating system
						  - exit: closes the shell""");
				case "exit", "endshell", "endcli" -> new ShellResult(EXIT);
				case "clear", "cls" -> new ShellResult(CLS);
				case "avail", "free" -> getMemorySpecs();
				case "pwd", "cd" -> getWorkingDirectory();
				case "uname" -> getOperatingSystem();
				case "gc" -> runGc();
				case "loadwb" -> new ShellResult(SUCCESS, "Not again!");
				default -> new ShellResult(UNKNOWN_COMMAND, arg);
			};
		}
		return new ShellResult(NO_OP);
	}

	private static ShellResult getMemorySpecs()
	{
		return new ShellResult(SUCCESS,
				"Memory allocated for the JVM: " + (Runtime.getRuntime().totalMemory() / 1024 / 1024) + " MB\n" +
						"Maximum allocatable memory: " + (Runtime.getRuntime().maxMemory() / 1024 / 1024) + " MB");
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

	/**
	 * [code borrowed from ant.jar]
	 * Crack a command line.
	 *
	 * @param toProcess the command line to process.
	 * @return the command line broken into strings.
	 * An empty or null toProcess parameter results in a zero sized array.
	 */
	public static String[] translateCommandline(String toProcess)
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
