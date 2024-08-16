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

import io.xeres.app.net.peer.PeerConnection;
import io.xeres.app.xrs.item.Item;
import io.xeres.common.events.NetworkReadyEvent;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;

/**
 * Base class for "Retroshare services".
 * These services have a unique number assigned which directs matching packets to them.
 * <p>
 * Note: this class has a natural ordering that is inconsistent with equals.
 */
@DependsOn({"rsServiceRegistry"})
public abstract class RsService implements Comparable<RsService>
{
	public abstract RsServiceType getServiceType();

	/**
	 * Handle incoming items. You can use JPA calls in there.
	 *
	 * @param sender the peer sending the item
	 * @param item   the item
	 */
	public abstract void handleItem(PeerConnection sender, Item item);

	private final RsServiceRegistry rsServiceRegistry;
	private boolean enabled;
	private boolean initialized;

	protected RsService(RsServiceRegistry rsServiceRegistry)
	{
		this.rsServiceRegistry = rsServiceRegistry;
	}

	public RsServiceInitPriority getInitPriority()
	{
		return RsServiceInitPriority.OFF;
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
	public void shutdown(PeerConnection peerConnection)
	{
		// Do nothing by default
	}

	/**
	 * Sent once when the application is exiting. Good place to perform spring boot related cleanups
	 * since the beans are all still available.
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
		enabled = rsServiceRegistry.registerService(this);
	}

	@EventListener
	public void init(NetworkReadyEvent event)
	{
		if (enabled && !initialized)
		{
			initialized = true;
			initialize();
			addSlavesIfNeeded();
		}
	}

	private void addSlavesIfNeeded()
	{
		if (RsServiceMaster.class.isAssignableFrom(getClass()))
		{
			//noinspection rawtypes,unchecked
			rsServiceRegistry.getSlaves(this).forEach(rsServiceSlave -> ((RsServiceMaster) this).addRsSlave(rsServiceSlave));
		}
	}

	@EventListener
	public void onApplicationEvent(ContextClosedEvent event)
	{
		if (enabled)
		{
			shutdown();
		}
	}

	@PreDestroy
	private void destroy()
	{
		if (enabled)
		{
			cleanup();
		}
	}

	@Override
	@SuppressWarnings("java:S1210")
	public int compareTo(RsService o)
	{
		return Integer.compare(getInitPriority().ordinal(), o.getInitPriority().ordinal());
	}

	@Override
	public String toString()
	{
		return getServiceType().getName();
	}
}
