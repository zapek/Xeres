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

package io.xeres.app.net.peer;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.ScheduledFuture;
import io.xeres.app.database.model.location.Location;
import io.xeres.app.xrs.service.RsService;
import io.xeres.common.util.NoSuppressedRunnable;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class PeerConnection
{
	private Location location;
	private final ChannelHandlerContext ctx;
	private final Set<RsService> services = new HashSet<>();
	private final AtomicBoolean servicesSent = new AtomicBoolean(false);
	private final Map<Integer, Object> data = new HashMap<>();
	private final Map<Integer, Map<Integer, Object>> serviceData = new HashMap<>();
	private final List<ScheduledFuture<?>> schedules = new ArrayList<>();

	public PeerConnection(Location location, ChannelHandlerContext ctx)
	{
		this.location = location;
		this.ctx = ctx;
	}

	public ChannelHandlerContext getCtx()
	{
		return ctx;
	}

	public Location getLocation()
	{
		return location;
	}

	public void updateLocation(Location location)
	{
		if (this.location.equals(location)) // Only update, don't change for another
		{
			this.location = location;
		}
	}

	public void addService(RsService service)
	{
		services.add(service);
	}

	public boolean isServiceSupported(RsService rsService)
	{
		return services.contains(rsService);
	}

	public boolean canSendServices()
	{
		return servicesSent.compareAndSet(false, true);
	}

	/**
	 * Puts data into a peer.
	 *
	 * @param key  the key
	 * @param data the data
	 */
	public void putData(int key, Object data)
	{
		this.data.put(key, data);
	}

	/**
	 * Gets data from a peer.
	 *
	 * @param key the key
	 * @return the data
	 */
	public Optional<Object> getData(int key)
	{
		return Optional.ofNullable(data.get(key));
	}

	/**
	 * Removes data from a peer.
	 *
	 * @param key the key
	 */
	public void removeData(int key)
	{
		data.remove(key);
	}

	/**
	 * Adds data specific to a service.
	 *
	 * @param service the service to add data to
	 * @param key     the key
	 * @param data    the data
	 */
	public void putServiceData(RsService service, int key, Object data)
	{
		serviceData.computeIfAbsent(service.getServiceType().getType(), k -> new HashMap<>()).put(key, data);
	}

	/**
	 * Gets data specific to a service.
	 *
	 * @param service the service to get data from
	 * @param key the key
	 * @return the data or an empty optional if there was none
	 */
	public Optional<Object> getServiceData(RsService service, int key)
	{
		var serviceMap = serviceData.get(service.getServiceType().getType());
		if (serviceMap == null)
		{
			return Optional.empty();
		}
		return Optional.ofNullable(serviceMap.get(key));
	}

	/**
	 * Removes data associated with the service.
	 *
	 * @param service the service to remove data from
	 * @param key the key
	 */
	public void removeServiceData(RsService service, int key)
	{
		var serviceMap = serviceData.get(service.getServiceType().getType());
		if (serviceMap != null)
		{
			serviceMap.remove(key);
		}
	}

	public void scheduleAtFixedRate(NoSuppressedRunnable command, long initialDelay, long period, TimeUnit unit)
	{
		@SuppressWarnings("resource") var scheduledFuture = ctx.executor().scheduleAtFixedRate(command, initialDelay, period, unit);
		schedules.add(scheduledFuture);
	}

	public void scheduleWithFixedDelay(NoSuppressedRunnable command, long initialDelay, long delay, TimeUnit unit)
	{
		@SuppressWarnings("resource") var scheduledFuture = ctx.executor().scheduleWithFixedDelay(command, initialDelay, delay, unit);
		schedules.add(scheduledFuture);
	}

	public void schedule(NoSuppressedRunnable command, long delay, TimeUnit unit)
	{
		@SuppressWarnings("resource") var scheduledFuture = ctx.executor().schedule(command, delay, unit);
		schedules.add(scheduledFuture);
	}

	public void shutdown()
	{
		services.forEach(rsService -> rsService.shutdown(this));
	}

	public void cleanup()
	{
		schedules.forEach(scheduledFuture -> scheduledFuture.cancel(false));
	}

	@Override
	public String toString()
	{
		return "PeerConnection{" +
				location +
				"@" + (ctx != null ? ctx.channel().remoteAddress() : "<unknown>") +
				'}';
	}
}
