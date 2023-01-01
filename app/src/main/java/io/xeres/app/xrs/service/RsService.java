/*
 * Copyright (c) 2019-2023 by David Gerber - https://zapek.com
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

package io.xeres.app.xrs.service;

import io.xeres.app.application.events.NetworkReadyEvent;
import io.xeres.app.net.peer.PeerConnection;
import io.xeres.app.xrs.item.Item;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Base class for "Retroshare services".
 * These services have a unique number assigned which directs matching packets to them.
 * <p>
 * Note: this class has a natural ordering that is inconsistent with equals.
 */
public abstract class RsService implements Comparable<RsService>
{
	public static final String RS_SERVICE_CLASS_SUFFIX = "RsService";
	private final Map<Integer, Class<? extends Item>> searchBySubType = new HashMap<>();
	private Map<Class<? extends Item>, Integer> searchByClass = new HashMap<>();

	public abstract RsServiceType getServiceType();

	public abstract Map<Class<? extends Item>, Integer> getSupportedItems();

	/**
	 * Handle incoming items. You can use JPA calls in there.
	 *
	 * @param sender the peer sending the item
	 * @param item   the item
	 */
	public abstract void handleItem(PeerConnection sender, Item item);

	private final Environment environment;
	private boolean initialized;

	protected RsService(Environment environment)
	{
		this.environment = environment;
	}

	public RsServiceInitPriority getInitPriority()
	{
		return RsServiceInitPriority.OFF;
	}

	public int getItemSubtype(Item item)
	{
		return searchByClass.get(item.getClass());
	}

	/**
	 * Sent once upon startup when the service is enabled and the network is ready. Good place to initialize
	 * executors, etc...
	 * <p>
	 * Keep in mind that your service can receive some packets before initialize() is called.
	 */
	public void initialize()
	{
		// Do nothing by default
	}

	/**
	 * Sent once when the application is exiting but before closing the connections.
	 * Good place to send last messages (for example, leaving a room, etc...).
	 */
	public void shutdown()
	{
		// Do nothing by default
	}

	/**
	 * Sent once when the application is almost done exiting. Good place to remove any
	 * executor setup in initialize().
	 */
	public void cleanup()
	{
		// Do nothing by default
	}

	public void initialize(PeerConnection peerConnection)
	{
		throw new IllegalStateException("Implement initialize() method if you override getInitPriority() to be anything else than OFF");
	}

	@PostConstruct
	private void init()
	{
		if (Boolean.TRUE.equals(environment.getProperty(getPropertyName(), Boolean.class, false)))
		{
			RsServiceRegistry.registerService(getServiceType().getType(), this);
			searchByClass = getSupportedItems();
			getSupportedItems().forEach((itemClass, itemSubType) ->
			{
				try
				{
					itemClass.getConstructor();
				}
				catch (NoSuchMethodException e)
				{
					throw new IllegalArgumentException(itemClass.getSimpleName() + " requires a public constructor with no parameters");
				}
				searchBySubType.put(itemSubType, itemClass);
			});
		}
	}

	@EventListener
	public void init(NetworkReadyEvent event)
	{
		if (!initialized)
		{
			initialized = true;
			initialize();
		}
	}

	@PreDestroy
	private void destroy()
	{
		cleanup();
	}

	public Item createItem(int subType) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException
	{
		var itemClass = searchBySubType.get(subType);
		if (itemClass == null)
		{
			throw new InstantiationException("No such type " + subType);
		}
		return itemClass.getConstructor().newInstance();
	}

	private String getPropertyName()
	{
		var className = getClass().getSimpleName();
		assert className.endsWith(RS_SERVICE_CLASS_SUFFIX);
		return "xrs.service." + className.substring(0, className.length() - RS_SERVICE_CLASS_SUFFIX.length()).toLowerCase(Locale.ROOT) + ".enabled";
	}

	@Override
	@SuppressWarnings("java:S1210")
	public int compareTo(RsService o)
	{
		return Integer.compare(getInitPriority().ordinal(), o.getInitPriority().ordinal());
	}
}
