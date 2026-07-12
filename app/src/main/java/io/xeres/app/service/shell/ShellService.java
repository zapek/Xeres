/*
 * Copyright (c) 2024-2026 by David Gerber - https://zapek.com
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
import io.xeres.app.service.InfoService;
import io.xeres.app.service.LocationService;
import io.xeres.app.service.script.ScriptService;
import io.xeres.app.xrs.service.forum.ForumRsService;
import io.xeres.app.xrs.service.gxs.GxsHelperService;
import io.xeres.app.xrs.service.identity.IdentityRsService;
import io.xeres.app.xrs.service.identity.item.IdentityGroupItem;
import io.xeres.common.AppName;
import io.xeres.common.annotation.VisibleForTesting;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.LocationIdentifier;
import io.xeres.common.mui.Shell;
import io.xeres.common.mui.ShellResult;
import io.xeres.common.protocol.xrs.RsServiceType;
import io.xeres.common.util.ByteUnitUtils;
import io.xeres.common.util.OsUtils;
import org.apache.commons.lang3.StringUtils;
import org.graalvm.polyglot.PolyglotException;
import org.slf4j.LoggerFactory;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static io.xeres.common.mui.ShellAction.*;

/**
 * Small shell to execute built-in commands. This is for advanced users for features
 * that don't warrant a full UI.
 */
@Service
public class ShellService implements Shell, SmartLifecycle
{
	private static final String COMMAND_HELP = "help";
	private static final String COMMAND_AVAIL = "avail";
	private static final String COMMAND_CLEAR = "clear";
	private static final String COMMAND_EXIT = "exit";
	private static final String COMMAND_PWD = "pwd";

	private final History history = new History(20);

	private static final String MEMORY_COLUMN = "%s  %10s %10s %10s\n";
	private static final String STATUS_COLUMN = "%-40s %-15s %-10s %-10s\n";

	private boolean running;

	private final ScriptService scriptService;
	private final ForumRsService forumRsService;
	private final GxsHelperService<?, ?> gxsHelperService;
	private final LocationService locationService;
	private final InfoService infoService;
	private final IdentityRsService identityRsService;
	private Runnable cleanupCommand;

	private final List<ShellCommand> shellCommands = List.of(
			new ShellCommand(COMMAND_HELP, "display this help", List.of(), this::runHelp),
			new ShellCommand("alias", "shows all the aliases", List.of(), this::runAlias),
			new ShellCommand(COMMAND_AVAIL, "shows the available memory", List.of(), this::runAvail),
			new ShellCommand("chkids", "checks all identities signatures", List.of(), this::runCheckIdentities),
			new ShellCommand(COMMAND_CLEAR, "clears the screen", List.of(), this::runClear),
			new ShellCommand("cpu", "shows the CPU count", List.of(), this::runCpu),
			new ShellCommand(COMMAND_EXIT, "closes the shell", List.of(), this::runExit),
			new ShellCommand("fixforums", "fixes forum duplicates", List.of(), this::runFixForums),
			new ShellCommand("fixpeermsgs", "resets last peer message update", List.of("location", "gxs", "service"), this::runFixPeerMessages),
			new ShellCommand("gc", "runs the garbage collector", List.of(), this::runGarbageCollector),
			new ShellCommand("loadwb", null, List.of(), this::runLoadWb),
			new ShellCommand("loglevel", "sets the log level of a package", List.of("package", "level"), this::runLogLevel),
			new ShellCommand("logs", "shows the logs", List.of(), this::runLogs),
			new ShellCommand("open", "opens a directory (app, cache, data or download)", List.of("directory"), this::runOpen),
			new ShellCommand("properties", "shows the properties", List.of(), this::runProperties),
			new ShellCommand(COMMAND_PWD, "shows the current directory", List.of(), this::runPrintWorkingDirectory),
			new ShellCommand("reload", "reloads user scripts", List.of(), this::runReload),
			new ShellCommand("status", "shows all threads", List.of(), this::runStatus),
			new ShellCommand("uname", "shows the operating system", List.of(), this::runUname),
			new ShellCommand("uptime", "shows the app uptime", List.of(), this::runUptime),
			new ShellCommand("version", "shows the app version", List.of(), this::runVersion)
	);

	private final List<ShellAlias> shellAliases = List.of(
			new ShellAlias(COMMAND_HELP, "?"),
			new ShellAlias(COMMAND_AVAIL, "free"),
			new ShellAlias(COMMAND_CLEAR, "cls"),
			new ShellAlias(COMMAND_EXIT, "endshell", "endcli", "quit", ":q!", ":q", "bye"),
			new ShellAlias(COMMAND_PWD, "cd")
	);

	public ShellService(ScriptService scriptService, ForumRsService forumRsService, GxsHelperService<?, ?> gxsHelperService, LocationService locationService, InfoService infoService, IdentityRsService identityRsService)
	{
		this.scriptService = scriptService;
		this.forumRsService = forumRsService;
		this.gxsHelperService = gxsHelperService;
		this.locationService = locationService;
		this.infoService = infoService;
		this.identityRsService = identityRsService;
	}

	@Override
	public void start()
	{
		running = true;
	}

	@Override
	public void stop()
	{
		running = false;
		if (cleanupCommand != null)
		{
			cleanupCommand.run();
		}
	}

	@Override
	public boolean isRunning()
	{
		return running;
	}

	@Override
	public ShellResult sendCommand(String input)
	{
		var args = new DefaultApplicationArguments(translateCommandline(input));
		var arg = args.getNonOptionArgs().isEmpty() ? null : args.getNonOptionArgs().getFirst();

		if (StringUtils.isAsciiPrintable(arg))
		{
			history.addCommand(input);
			try
			{
				var shellCommand = findCommand(findAlias(arg.toLowerCase(Locale.ROOT)));
				return shellCommand.command().apply(args);
			}
			catch (Exception e)
			{
				return new ShellResult(ERROR, e.getMessage());
			}
		}
		return new ShellResult(NO_OP);
	}

	@Override
	public void registerCleanup(Runnable cleanupCommand)
	{
		this.cleanupCommand = cleanupCommand;
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

	private String findAlias(String input)
	{
		return shellAliases.stream()
				.filter(shellAlias -> Arrays.stream(shellAlias.alias()).toList().contains(input))
				.map(ShellAlias::name)
				.findAny().orElse(input);
	}

	private ShellCommand findCommand(String input)
	{
		return shellCommands.stream()
				.filter(command -> command.name().equals(input))
				.findFirst()
				.orElseGet(() -> new ShellCommand(null, null, List.of(), this::showUnknown));
	}

	/**
	 * Gets the argument.
	 *
	 * @param args  the arguments
	 * @param index the index of the argument, 0 for the command name, 1 for the first argument, etc...
	 * @return the argument or null if it wasn't supplied
	 */
	private String getArgument(DefaultApplicationArguments args, int index)
	{
		return args.getNonOptionArgs().size() > index ? args.getNonOptionArgs().get(index) : null;
	}

	private ShellResult runHelp(DefaultApplicationArguments args)
	{
		var sb = new StringBuilder("Available commands:\n");
		for (var command : shellCommands)
		{
			if (!StringUtils.isBlank(command.description()))
			{
				sb.append("  - ");
				sb.append(command.name());
				if (!command.arguments().isEmpty())
				{
					for (var arg : command.arguments())
					{
						sb.append(" [");
						sb.append(arg);
						sb.append("]");
					}
				}
				sb.append(": ");
				sb.append(command.description());
				sb.append("\n");
			}
		}
		return new ShellResult(SUCCESS, sb.toString());
	}

	private ShellResult runExit(DefaultApplicationArguments args)
	{
		return new ShellResult(EXIT);
	}

	private ShellResult runClear(DefaultApplicationArguments args)
	{
		return new ShellResult(CLS);
	}

	private ShellResult showUnknown(DefaultApplicationArguments args)
	{
		assert !args.getNonOptionArgs().isEmpty();
		return new ShellResult(UNKNOWN_COMMAND, args.getNonOptionArgs().getFirst());
	}

	private ShellResult runAlias(DefaultApplicationArguments args)
	{
		var sb = new StringBuilder();
		for (var alias : shellAliases)
		{
			sb.append(alias.name());
			sb.append(": ");
			for (var s : alias.alias())
			{
				sb.append(s);
				sb.append(" ");
			}
			sb.append("\n");
		}
		return new ShellResult(SUCCESS, sb.toString());
	}

	private ShellResult runProperties(DefaultApplicationArguments args)
	{
		var properties = System.getProperties();

		var map = properties.entrySet().stream()
				.collect(Collectors.toMap(k -> (String) k.getKey(), e -> (String) e.getValue()))
				.entrySet().stream()
				.sorted(Map.Entry.comparingByKey())
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
						(oldValue, _) -> oldValue, LinkedHashMap::new));

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

	private ShellResult runAvail(DefaultApplicationArguments args)
	{
		var totalMemory = Runtime.getRuntime().totalMemory();

		var sb = new StringBuilder("Type   Available     In-Use    Maximum\n");
		sb.append(String.format(MEMORY_COLUMN, "HEAP",
				ByteUnitUtils.fromBytes(totalMemory),
				ByteUnitUtils.fromBytes(totalMemory - Runtime.getRuntime().freeMemory()),
				ByteUnitUtils.fromBytes(Runtime.getRuntime().maxMemory())));

		var supportedPools = Map.of(
				"Metaspace", "META",
				"CodeCache", "CODE");

		for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans())
		{
			var heading = supportedPools.get(pool.getName());
			if (heading != null)
			{
				var usage = pool.getUsage();
				sb.append(String.format(MEMORY_COLUMN, heading,
						ByteUnitUtils.fromBytes(usage.getCommitted()),
						ByteUnitUtils.fromBytes(usage.getUsed()),
						usage.getMax() >= 0 ? ByteUnitUtils.fromBytes(usage.getMax()) : "-"));
			}
		}

		var supportedBuffers = Map.of(
				"mapped", "MMAP",
				"direct", "DIRE"
		);

		for (BufferPoolMXBean pool : ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class))
		{
			var heading = supportedBuffers.get(pool.getName());
			if (heading != null)
			{
				sb.append(String.format(MEMORY_COLUMN, heading,
						"-",
						ByteUnitUtils.fromBytes(pool.getMemoryUsed()),
						"-"));
			}
		}

		return new ShellResult(SUCCESS, sb.toString());
	}

	private ShellResult runStatus(DefaultApplicationArguments args)
	{
		var threadSet = Thread.getAllStackTraces().keySet().stream().sorted(Comparator.comparing(Thread::getName)).toList();

		var sb = new StringBuilder(String.format(STATUS_COLUMN, "Name", "State", "Priority", "Daemon"));
		sb.append("--------------------------------------------------------------------------\n");

		for (Thread t : threadSet)
		{
			var name = t.getName();

			sb.append(String.format(STATUS_COLUMN,
					name.length() > 40 ? name.substring(0, 39) + "…" : name,
					t.getState(),
					t.getPriority(),
					t.isDaemon() ? "Y" : "N"));
		}
		sb.append("\nTotal threads: ");
		sb.append(threadSet.size());
		return new ShellResult(SUCCESS, sb.toString());
	}

	private ShellResult runUptime(DefaultApplicationArguments args)
	{
		var duration = infoService.getUptime();

		var days = duration.toDays();
		var hours = duration.toHours() % 24;
		var minutes = duration.toMinutes() % 60;
		var seconds = duration.getSeconds() % 60;

		return new ShellResult(SUCCESS,
				String.format("%d days, %d hours, %d minutes, %d seconds",
						days, hours, minutes, seconds));
	}

	private ShellResult runCpu(DefaultApplicationArguments args)
	{
		return new ShellResult(SUCCESS,
				"CPU count: " + Runtime.getRuntime().availableProcessors());
	}

	private ShellResult runPrintWorkingDirectory(DefaultApplicationArguments args)
	{
		return new ShellResult(SUCCESS, System.getProperty("user.dir"));
	}

	private ShellResult runUname(DefaultApplicationArguments args)
	{
		return new ShellResult(SUCCESS, System.getProperty("os.name") + " (" + System.getProperty("os.arch") + ")");
	}

	private ShellResult runGarbageCollector(DefaultApplicationArguments args)
	{
		System.gc();
		return new ShellResult(SUCCESS, "Done");
	}

	private ShellResult runLogLevel(DefaultApplicationArguments args)
	{
		var packageName = getArgument(args, 1);
		var level = getArgument(args, 2);
		if (StringUtils.isBlank(packageName))
		{
			throw new IllegalArgumentException("package name must be provided (eg. io.xeres.app.application.Startup)");
		}
		if (StringUtils.isBlank(level))
		{
			throw new IllegalArgumentException("log level must be provided (trace, debug, info, warn, error)");
		}

		var loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		var logger = loggerContext.exists(packageName);
		Objects.requireNonNull(logger, "no such logger");

		logger.setLevel(Level.valueOf(level));

		return new ShellResult(SUCCESS, "Level of " + logger.getName() + " changed to " + logger.getLevel());
	}

	private ShellResult runLogs(DefaultApplicationArguments args)
	{
		OsUtils.shellOpen(OsUtils.getLogFile().toFile());
		return new ShellResult(SUCCESS, "Showing logs in external viewer");
	}

	private ShellResult runOpen(DefaultApplicationArguments args)
	{
		var name = getArgument(args, 1);

		Path directory;

		directory = switch (name)
		{
			case "app" -> OsUtils.getApplicationHome();
			case "cache" -> OsUtils.getCacheDir();
			case "data" -> OsUtils.getDataDir();
			case "download" -> OsUtils.getDownloadDir();
			case null, default -> null;
		};

		Objects.requireNonNull(directory, "Invalid directory name. Must be either 'app', 'cache, 'data' or 'download'");

		OsUtils.showFolder(directory.toFile());

		return new ShellResult(SUCCESS, "Opening " + name + " directory at " + directory + " ...");
	}

	private ShellResult runReload(DefaultApplicationArguments args)
	{
		try
		{
			scriptService.reload();
		}
		catch (PolyglotException e)
		{
			var sw = new StringWriter();
			var pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			return new ShellResult(ERROR, "Reload failed: " + sw);
		}
		return new ShellResult(SUCCESS, "Reloaded");
	}

	private ShellResult runVersion(DefaultApplicationArguments args)
	{
		return new ShellResult(SUCCESS, AppName.NAME + " " + infoService.getVersion());
	}

	private ShellResult runFixForums(DefaultApplicationArguments args)
	{
		forumRsService.fixDuplicates();
		return new ShellResult(SUCCESS, "Fixed forum duplicates");
	}

	private ShellResult runFixPeerMessages(DefaultApplicationArguments args)
	{
		var locationString = getArgument(args, 1);
		var gxsIdString = getArgument(args, 2);
		var serviceTypeString = getArgument(args, 3);

		var location = Objects.requireNonNull(locationService.findLocationByLocationIdentifier(LocationIdentifier.fromString(locationString)).orElse(null), "Invalid location identifier");
		var gxsId = GxsId.fromString(gxsIdString);
		if (gxsId.isNullIdentifier())
		{
			throw new IllegalArgumentException("Invalid group identifier");
		}
		var rsServiceType = RsServiceType.fromName(serviceTypeString);
		if (rsServiceType == RsServiceType.NONE)
		{
			throw new IllegalArgumentException("Invalid service type, must be one of " + Arrays.stream(RsServiceType.values())
					.sorted()
					.map(Enum::name)
					.filter(s -> s.startsWith("GXS_"))
					.collect(Collectors.joining(", ")));
		}

		gxsHelperService.setLastPeerMessageUpdate(location, gxsId, Instant.EPOCH, rsServiceType);

		return new ShellResult(SUCCESS, "Successfully reset peer update time");
	}

	private ShellResult runCheckIdentities(DefaultApplicationArguments args)
	{
		var invalidIdentities = identityRsService.getInvalidIdentities();
		if (invalidIdentities.isEmpty())
		{
			return new ShellResult(SUCCESS, "No invalid identities found");
		}

		var sb = new StringBuilder("Invalid identities:\n");
		for (var group : invalidIdentities.stream().sorted(Comparator.comparing(IdentityGroupItem::getName, String.CASE_INSENSITIVE_ORDER)).toList())
		{
			sb.append(String.format("%-32s %32s\n", group.getName(), group.getGxsId().asString()));
		}
		sb.append("Count: ");
		sb.append(invalidIdentities.size());
		return new ShellResult(SUCCESS, sb.toString());
	}

	private ShellResult runLoadWb(DefaultApplicationArguments args)
	{
		return new ShellResult(SUCCESS, "Not again!");
	}

	/**
	 * [code borrowed from ant.jar]
	 * Crack a command line.
	 *
	 * @param toProcess the command line to process.
	 * @return the command line broken into strings.
	 * An empty or null toProcess parameter results in a zero-sized array.
	 */
	@VisibleForTesting
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
}
