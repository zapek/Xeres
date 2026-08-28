/*
 * Copyright (c) 2025-2026 by David Gerber - https://zapek.com
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

package io.xeres.app.service.script;

import io.xeres.app.application.environment.DataDirLocator;
import io.xeres.common.util.ExecutorUtils;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/// A service to run JS scripts.
@Service
public class ScriptService implements SmartLifecycle
{
	private static final Logger log = LoggerFactory.getLogger(ScriptService.class);

	private boolean running;

	private Context context;
	private final AtomicBoolean initialized = new AtomicBoolean(false);
	private final BlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<>();
	private Thread jsThread;

	private final Environment environment;
	private final JsXeres jsXeres;

	private ScheduledExecutorService scheduledExecutorService;

	public ScriptService(Environment environment, JsXeres jsXeres)
	{
		this.environment = environment;
		this.jsXeres = jsXeres;
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
		closeContext();
	}

	@Override
	public boolean isRunning()
	{
		return running;
	}

	/// Reloads all scripts.
	public void reload()
	{
		closeContext();
		startContext();
	}

	/// Starts the scripts. The context is created and all its code is executed on the JavaScript
	/// runner thread, the only thread allowed to access the JS context (see [runOnJsThread]).
	private void startContext()
	{
		if (initialized.get())
		{
			return;
		}

		ensureJsThread();
		runOnJsThread(this::initContext);
	}

	private void initContext()
	{
		scheduledExecutorService = ExecutorUtils.createExecutor();

		Path scriptPath;

		if (environment.acceptsProfiles(Profiles.of("dev")))
		{
			scriptPath = Path.of("./scripts/api/user.js");
		}
		else
		{
			if (DataDirLocator.getDataDir() == null) // Don't run for tests
			{
				return;
			}
			scriptPath = Path.of(DataDirLocator.getDataDir(), "Scripts/user.js");
		}

		if (!scriptPath.toFile().isFile())
		{
			log.info("Script file not found: {}", scriptPath);
			return;
		}

		context = Context.newBuilder("js")
				.option("js.strict", "true")
				.option("js.console", "false") // Default console uses stdout/stderr which we don't want
				.allowAllAccess(true) // For now, will need tweaking (XXX: remove and make safer)
				.build();

		String scriptContent;

		try
		{
			scriptContent = new String(Files.readAllBytes(scriptPath));
		}
		catch (IOException e)
		{
			log.error("Error reading script file: {}", scriptPath, e);
			return;
		}

		// Expose some APIs to the JavaScript script (members class needs to be public)
		var bindings = context.getBindings("js");

		// The Xeres API
		bindings.putMember("xeres", jsXeres);

		// Console replacement
		bindings.putMember("console", new JsConsole());

		// Timer functions are top level methods, we cannot just use an object
		var timer = new JsTimer(scheduledExecutorService, value -> runOnJsThread(value::execute));
		bindings.putMember("setInterval", (ProxyExecutable) args -> timer.setInterval(args[0], args[1].asInt()));
		bindings.putMember("clearInterval", (ProxyExecutable) args -> timer.clearInterval(args[0].asInt()));
		bindings.putMember("setTimeout", (ProxyExecutable) args -> timer.setTimeout(args[0], args[1].asInt()));
		bindings.putMember("clearTimeout", (ProxyExecutable) args -> timer.clearTimeout(args[0].asInt()));

		var jsFetch = new JsFetch(context, this::runOnJsThread);
		bindings.putMember("fetch", (ProxyExecutable) args -> jsFetch.fetch(args[0], args.length > 1 ? args[1] : null));

		// Execute the script
		try
		{
			context.eval("js", scriptContent);
		}
		catch (PolyglotException e)
		{
			log.error("Error in script {}", scriptPath, e);
		}
		initialized.set(true);
	}

	/// Sends an event to JS
	///
	/// @param type the type of event
	/// @param data the data
	public void sendEvent(String type, Object data)
	{
		if (!initialized.get())
		{
			return;
		}
		runOnJsThread(() -> processEvent(new ScriptEvent(type, data)));
	}

	/// Runs a task on the JavaScript runner thread, which is the single thread allowed to access the
	/// JS context. Timers and other callbacks must go through here to avoid multi-threaded context access.
	public void runOnJsThread(Runnable runnable)
	{
		taskQueue.add(runnable);
	}

	/// Ensures the JavaScript runner thread exists. All JS context access (creation, evaluation, event
	/// handlers, timers, fetch) is confined to this single platform thread, so that the context is never
	/// entered concurrently. It is (re)started on start/reload and interrupted on stop/close.
	private void ensureJsThread()
	{
		if (jsThread != null && jsThread.isAlive())
		{
			return;
		}
		// We use platform threads, because using polyglot contexts on Java virtual threads on HotSpot is experimental in this release,
		// because access to caller frames in write or materialize mode is not yet supported on virtual threads. (Some tools and languages depend on that.)
		jsThread = Thread.ofPlatform()
				.name("JavaScript Runner")
				.start(() -> {
					while (!Thread.currentThread().isInterrupted())
					{
						try
						{
							Runnable task = taskQueue.take();
							task.run();
						}
						catch (InterruptedException _)
						{
							Thread.currentThread().interrupt();
							break;
						}
					}
				});
	}

	private void processEvent(ScriptEvent event)
	{
		try
		{
			// Check if the script has a handler for this event type
			Value handler = jsXeres.getEventHandler(event.type());
			if (handler != null && handler.canExecute())
			{
				// Convert Java data to JavaScript value
				var jsData = convertToJsValue(event.data());
				handler.execute(jsData);
			}
		}
		catch (PolyglotException e)
		{
			log.error("Error processing event {}", event, e);
		}
	}

	private Value convertToJsValue(Object data)
	{
		if (data instanceof Map)
		{
			@SuppressWarnings("unchecked")
			Map<String, Object> map = (Map<String, Object>) data;
			var proxyMap = ProxyObject.fromMap(map);
			return context.asValue(proxyMap);
		}
		if (data instanceof List)
		{
			@SuppressWarnings("unchecked")
			List<Object> list = (List<Object>) data;
			var proxyArray = ProxyArray.fromList(list);
			return context.asValue(proxyArray);
		}
		return context.asValue(data);
	}

	private void closeContext()
	{
		initialized.set(false);

		ExecutorUtils.cleanupExecutor(scheduledExecutorService);

		if (jsThread != null)
		{
			jsThread.interrupt();
			jsThread = null;
		}
		if (context != null)
		{
			context.close();
			context = null;
		}
	}
}
