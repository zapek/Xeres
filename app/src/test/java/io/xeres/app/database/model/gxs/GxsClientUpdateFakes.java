package io.xeres.app.database.model.gxs;

import io.xeres.app.database.model.location.Location;
import io.xeres.app.database.model.location.LocationFakes;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

public final class GxsClientUpdateFakes
{
	private GxsClientUpdateFakes()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static GxsClientUpdate createGxsClientUpdate()
	{
		return createGxsClientUpdate(LocationFakes.createLocation(), ThreadLocalRandom.current().nextInt(1, 200));
	}

	public static GxsClientUpdate createGxsClientUpdate(Location location, int serviceType)
	{
		return new GxsClientUpdate(location, serviceType, Instant.now());
	}
}
