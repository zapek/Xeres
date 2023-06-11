/*
 * Copyright (c) 2023 by David Gerber - https://zapek.com
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

import io.xeres.app.database.model.gxs.GxsGroupItem;
import io.xeres.app.database.model.gxs.GxsMessageItem;
import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.service.gxs.GxsRsService;
import io.xeres.app.xrs.service.gxs.item.DynamicServiceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

@Component
public class RsServiceRegistry
{
	private static final Logger log = LoggerFactory.getLogger(RsServiceRegistry.class);
	private static final String SERVICE_PACKAGE = "io.xeres.app.xrs.service";
	private static final String RS_SERVICE_CLASS_SUFFIX = "RsService";

	private final Set<String> enabledServiceClasses = new HashSet<>();
	private final Map<Integer, RsService> services = new HashMap<>();

	private final Map<Integer, Map<Integer, Class<? extends Item>>> itemClassesWaiting = new HashMap<>();
	private final Map<Integer, Class<? extends Item>> itemClassesGxsWaiting = new HashMap<>();
	private final Map<Integer, Class<? extends Item>> itemClasses = new HashMap<>();

	public RsServiceRegistry(Environment environment)
	{
		var provider = new ClassPathScanningCandidateComponentProvider(false);
		provider.addIncludeFilter(new AssignableTypeFilter(RsService.class));
		var scannedServiceClasses = provider.findCandidateComponents(SERVICE_PACKAGE);

		provider.resetFilters(false);
		provider.addIncludeFilter(new AssignableTypeFilter(Item.class));
		var scannedItemClasses = provider.findCandidateComponents(SERVICE_PACKAGE);

		// Record which services are enabled in the properties file.
		for (var bean : scannedServiceClasses)
		{
			try
			{
				@SuppressWarnings("unchecked")
				var serviceClass = (Class<? extends RsService>) Class.forName(bean.getBeanClassName());
				var serviceName = serviceClass.getSimpleName();
				var propertyName = "xrs.service." + serviceName.substring(0, serviceName.length() - RS_SERVICE_CLASS_SUFFIX.length()).toLowerCase(Locale.ROOT) + ".enabled";
				if (Boolean.TRUE.equals(environment.getProperty(propertyName, Boolean.class, false)))
				{
					enabledServiceClasses.add(serviceName);
				}
			}
			catch (ClassNotFoundException e)
			{
				throw new RuntimeException(e);
			}
		}

		// Add all item classes, they will be enabled later when the service is confirmed to be enabled
		for (var bean : scannedItemClasses)
		{
			Class<? extends Item> itemClass = null;

			try
			{
				//noinspection unchecked
				itemClass = (Class<? extends Item>) Class.forName(bean.getBeanClassName());

				var item = (Item) itemClass.getConstructor().newInstance();

				if (GxsGroupItem.class.isAssignableFrom(itemClass) || GxsMessageItem.class.isAssignableFrom(itemClass))
				{
					// For GxsGroup and GxsMessage items, we ignore them because they can only be received within transactions
					// (but the real reason is that their subtype clashes with GxsExchange subtypes)
				}
				else if (DynamicServiceType.class.isAssignableFrom(itemClass))
				{
					// For DynamicServiceType (mostly GxsExchange) items, we don't know their ServiceType yet because they are shared.
					itemClassesGxsWaiting.put(item.getSubType(), itemClass);
				}
				else
				{
					itemClassesWaiting.computeIfAbsent(item.getServiceType(), k -> new HashMap<>()).put(item.getSubType(), itemClass);
				}
			}
			catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException | ClassNotFoundException e)
			{
				if (itemClass != null)
				{
					throw new IllegalArgumentException(itemClass.getSimpleName() + " requires a public constructor with no parameters");
				}
				else
				{
					throw new RuntimeException(e);
				}
			}
		}
	}

	public List<RsService> getServices()
	{
		return new ArrayList<>(services.values());
	}

	public RsService getServiceFromType(int type)
	{
		return services.get(type);
	}

	public boolean registerService(RsService rsService)
	{
		var serviceType = rsService.getServiceType().getType();

		if (!enabledServiceClasses.contains(rsService.getClass().getSimpleName()))
		{
			return false; // the service is disabled
		}

		services.put(serviceType, rsService);

		if (GxsRsService.class.isAssignableFrom(rsService.getClass()))
		{
			itemClassesGxsWaiting.forEach((subType, itemClass) -> itemClasses.put(serviceType << 16 | subType, itemClass));
		}
		else
		{
			var itemClassMap = itemClassesWaiting.remove(serviceType);
			if (itemClassMap != null)
			{
				itemClassMap.forEach((subType, itemClass) -> itemClasses.put(serviceType << 16 | subType, itemClass));
			}
		}
		return true;
	}

	public Item buildIncomingItem(int version, int service, int subtype)
	{
		if (version == 2)
		{
			var itemClass = itemClasses.get(service << 16 | subtype);
			if (itemClass != null)
			{
				try
				{
					var item = itemClass.getConstructor().newInstance();
					if (DynamicServiceType.class.isAssignableFrom(item.getClass()))
					{
						((DynamicServiceType) item).setServiceType(service);
					}
					return item;
				}
				catch (InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException e)
				{
					log.error("Couldn't create item: {}", e.getMessage());
				}
			}
			else
			{
				log.warn("Couldn't create item (service: {}, subtype: {}): no mapping found", service, subtype);
			}
		}
		else
		{
			log.warn("Packet version {} is not supported", version);
		}
		return new DefaultItem(); // will just get disposed
	}
}
