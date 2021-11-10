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

package io.xeres.app.xrs.service;

import io.netty.channel.ChannelFuture;
import io.xeres.app.database.model.location.Location;
import io.xeres.app.net.peer.PeerConnection;
import io.xeres.app.net.peer.PeerConnectionManager;
import io.xeres.app.xrs.item.Item;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Note: this class has a natural ordering that is inconsistent with equals.
 */
public abstract class RsService implements Comparable<RsService>
{
	private final Map<Integer, Class<? extends Item>> searchBySubType = new HashMap<>();
	private Map<Class<? extends Item>, Integer> searchByClass = new HashMap<>();

	public abstract RsServiceType getServiceType();

	public abstract Map<Class<? extends Item>, Integer> getSupportedItems();

	public abstract void handleItem(PeerConnection sender, Item item);

	private final Environment environment;
	private final PeerConnectionManager peerConnectionManager;

	protected RsService(Environment environment, PeerConnectionManager peerConnectionManager)
	{
		this.environment = environment;
		this.peerConnectionManager = peerConnectionManager;
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
	 * Sent once upon startup when the service is enabled. Good place to initialize
	 * executors, etc...
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
		Class<? extends Item> itemClass = searchBySubType.get(subType);
		if (itemClass == null)
		{
			throw new InstantiationException("No such type " + subType);
		}
		return itemClass.getConstructor().newInstance();
	}

	private String getPropertyName()
	{
		String className = getClass().getSimpleName();
		assert className.endsWith("Service");
		return "xrs.service." + className.substring(0, className.length() - 7).toLowerCase(Locale.ROOT) + ".enabled";
	}

	protected ChannelFuture writeItem(PeerConnection peerConnection, Item item)
	{
		return peerConnectionManager.writeItem(peerConnection, item, this);
	}

	protected ChannelFuture writeItem(Location location, Item item)
	{
		return peerConnectionManager.writeItem(location, item, this);
	}

	@Override
	@SuppressWarnings("java:S1210")
	public int compareTo(RsService o)
	{
		return Integer.compare(getInitPriority().ordinal(), o.getInitPriority().ordinal());
	}
}
