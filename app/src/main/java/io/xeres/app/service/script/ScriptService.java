/*
 * Copyright (c) 2025 by David Gerber - https://zapek.com
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

import io.xeres.app.configuration.DataDirConfiguration;
import io.xeres.app.service.IdentityService;
import io.xeres.app.service.MessageService;
import io.xeres.app.xrs.service.chat.ChatRsService;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.LocationIdentifier;
import io.xeres.common.message.MessageType;
import io.xeres.common.message.chat.ChatMessage;
import io.xeres.common.message.chat.ChatRoomMessage;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.xeres.common.message.MessagePath.*;

/// A service to run JS scripts.
@Service
public class ScriptService
{
	private static final Logger log = LoggerFactory.getLogger(ScriptService.class);

	private Context context;
	private final Map<String, Value> eventHandlers = new ConcurrentHashMap<>();
	private final AtomicBoolean initialized = new AtomicBoolean(false);
	private final BlockingQueue<ScriptEvent> eventQueue = new LinkedBlockingQueue<>();
	private Thread eventProcessorThread;

	private final Environment environment;
	private final DataDirConfiguration dataDirConfiguration;
	private final ChatRsService chatRsService;
	private final MessageService messageService;
	private final IdentityService identityService;

	public ScriptService(Environment environment, DataDirConfiguration dataDirConfiguration, @Lazy ChatRsService chatRsService, MessageService messageService, IdentityService identityService)
	{
		this.environment = environment;
		this.dataDirConfiguration = dataDirConfiguration;
		this.chatRsService = chatRsService;
		this.messageService = messageService;
		this.identityService = identityService;
	}

	@PostConstruct
	private void init()
	{
		startContext(false);
	}

	/// Reloads all scripts.
	public void reload()
	{
		closeContext();
		startContext(true);
	}

	private void startContext(boolean throwIfErrors)
	{
		if (initialized.get())
		{
			return;
		}

		Path scriptPath;

		if (environment.acceptsProfiles(Profiles.of("dev")))
		{
			scriptPath = Path.of("./scripts/api/user.js");
		}
		else
		{
			if (dataDirConfiguration.getDataDir() == null) // Don't run for tests
			{
				return;
			}
			scriptPath = Path.of(dataDirConfiguration.getDataDir(), "Scripts/user.js");
		}

		if (!scriptPath.toFile().isFile())
		{
			log.info("Script file not found: {}", scriptPath);
			return;
		}

		context = Context.newBuilder("js")
				.option("js.strict", "true")
				.option("js.console", "false")
				.allowAllAccess(true)
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

		// Expose some APIs to the JavaScript script
		context.getBindings("js").putMember("xeresAPI", new XeresAPI());
		context.getBindings("js").putMember("console", new Console());

		// Execute the script
		try
		{
			context.eval("js", scriptContent);
		}
		catch (PolyglotException e)
		{
			if (throwIfErrors)
			{
				throw e;
			}
			else
			{
				log.error("Error in script {}", scriptPath, e);
			}
		}
		initialized.set(true);
		startEventProcessor();
	}

	private void startEventProcessor()
	{
		// We use platform threads, because using polyglot contexts on Java virtual threads on HotSpot is experimental in this release,
		// because access to caller frames in write or materialize mode is not yet supported on virtual threads (some tools and languages depend on that).
		eventProcessorThread = Thread.ofPlatform()
				.name("JavaScript Runner")
				.start(() -> {
					while (initialized.get() && !Thread.currentThread().isInterrupted())
					{
						try
						{
							ScriptEvent event = eventQueue.take();
							processEvent(event);
						}
						catch (InterruptedException _)
						{
							Thread.currentThread().interrupt();
							break;
						}
					}
				});
	}

	public void sendEvent(String type, Object data)
	{
		if (!initialized.get())
		{
			return;
		}
		eventQueue.add(new ScriptEvent(type, data));
	}

	private void processEvent(ScriptEvent event)
	{
		try
		{
			// Check if the script has a handler for this event type
			Value handler = eventHandlers.get(event.type());
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

	@PreDestroy
	private void shutdown()
	{
		closeContext();
	}

	private void closeContext()
	{
		initialized.set(false);
		if (eventProcessorThread != null)
		{
			eventProcessorThread.interrupt();
		}
		if (context != null)
		{
			context.close();
		}
	}

	/// The Xeres API callable by JS scripts.
	@SuppressWarnings("unused") // All methods here can be used by JS
	public class XeresAPI
	{
		/// Registers an event handler. Those are called by Xeres.
		///
		/// @param eventType the event type
		/// @param handler   the handler
		public void registerEventHandler(String eventType, Value handler)
		{
			eventHandlers.put(eventType, handler);
		}

		/// Sends a message to a chat room.
		///
		/// @param roomId  the room id
		/// @param message the message
		public void sendChatRoomMessage(long roomId, String message)
		{
			chatRsService.sendChatRoomMessage(roomId, message);
			messageService.sendToConsumers(chatRoomDestination(), MessageType.CHAT_ROOM_MESSAGE, roomId, new ChatRoomMessage(identityService.getOwnIdentity().getName(), identityService.getOwnIdentity().getGxsId(), message));
		}

		/// Sends a private chat message.
		///
		/// @param destination the destination (location)
		/// @param message     the message
		public void sendPrivateMessage(String destination, String message)
		{
			var location = LocationIdentifier.fromString(destination);
			chatRsService.sendPrivateMessage(location, message);
			var chatMessage = new ChatMessage(message);
			chatMessage.setOwn(true);
			messageService.sendToConsumers(chatPrivateDestination(), MessageType.CHAT_PRIVATE_MESSAGE, location, chatMessage);
		}

		/// Sends a distant chat message.
		///
		/// @param destination the destination (gxsId)
		/// @param message     the message
		public void sendDistantMessage(String destination, String message)
		{
			var gxsId = GxsId.fromString(destination);
			chatRsService.sendPrivateMessage(gxsId, message);
			var chatMessage = new ChatMessage(message);
			chatMessage.setOwn(true);
			messageService.sendToConsumers(chatDistantDestination(), MessageType.CHAT_PRIVATE_MESSAGE, gxsId, chatMessage);
		}
	}
}
