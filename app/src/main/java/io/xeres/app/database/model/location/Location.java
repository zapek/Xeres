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

package io.xeres.app.database.model.location;

import io.xeres.app.crypto.rsid.RSId;
import io.xeres.app.crypto.rsid.RSIdBuilder;
import io.xeres.app.database.converter.NetModeConverter;
import io.xeres.app.database.model.connection.Connection;
import io.xeres.app.database.model.gxs.GxsClientUpdate;
import io.xeres.app.database.model.profile.Profile;
import io.xeres.app.net.protocol.PeerAddress;
import io.xeres.app.service.backup.LocationIdXmlAdapter;
import io.xeres.app.service.backup.RSIdXmlAdapter;
import io.xeres.common.id.LocationId;
import io.xeres.common.protocol.NetMode;
import io.xeres.common.rsid.Type;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static io.xeres.common.dto.location.LocationConstants.OWN_LOCATION_ID;
import static java.util.Comparator.*;

@Entity
@XmlAccessorType(XmlAccessType.NONE)
public class Location
{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "profile_id", nullable = false)
	private Profile profile;

	@NotNull
	private String name;

	@Embedded
	@NotNull
	@AttributeOverride(name = "identifier", column = @Column(name = "location_identifier"))
	private LocationId locationId;

	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "location", orphanRemoval = true)
	private final List<Connection> connections = new ArrayList<>();

	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "location", orphanRemoval = true)
	private final List<GxsClientUpdate> clientUpdates = new ArrayList<>();

	private boolean connected;

	private Instant lastConnected;

	private boolean discoverable = true;

	private boolean dht = true;

	@Transient
	private String version;

	@Convert(converter = NetModeConverter.class)
	private NetMode netMode = NetMode.UNKNOWN;

	protected Location()
	{

	}

	protected Location(String name)
	{
		this.name = name;
	}

	protected Location(long id, String name, Profile profile, LocationId locationId)
	{
		this.id = id;
		this.name = name;
		this.profile = profile;
		this.locationId = locationId;
	}

	protected Location(String name, Profile profile, LocationId locationId)
	{
		this.name = name;
		this.profile = profile;
		this.locationId = locationId;
	}

	public static Location createLocation(RSId rsId)
	{
		return new Location(rsId);
	}

	public static Location createLocation(String name)
	{
		return new Location(name);
	}

	public static Location createLocation(String name, Profile profile, LocationId locationId)
	{
		return new Location(name, profile, locationId);
	}

	public static Location createLocation(String name, LocationId locationId)
	{
		var location = new Location(name);
		location.setLocationId(locationId);
		return location;
	}

	public static void addOrUpdateLocations(Profile profile, Location newLocation)
	{
		profile.getLocations().removeIf(oldLocation -> oldLocation.getLocationId().equals(newLocation.getLocationId())); // XXX: don't remove but update if there are additional fields that were gathered before an update (ie. additional IPs)
		profile.addLocation(newLocation);
	}

	public Location(RSId rsId)
	{
		setName(rsId.getName());
		setLocationId(rsId.getLocationId());
		rsId.getDnsName().ifPresent(peerAddress -> addConnection(Connection.from(peerAddress)));
		rsId.getInternalIp().ifPresent(peerAddress -> addConnection(Connection.from(peerAddress)));
		rsId.getExternalIp().ifPresent(peerAddress -> addConnection(Connection.from(peerAddress)));
		rsId.getHiddenNodeAddress().ifPresent(peerAddress -> addConnection(Connection.from(peerAddress)));

		rsId.getLocators().forEach(peerAddress -> addConnection(Connection.from(peerAddress)));
	}

	@XmlAttribute
	@XmlJavaTypeAdapter(RSIdXmlAdapter.class)
	@Transient
	public RSId getCertificate()
	{
		return getRsId(Type.CERTIFICATE);
	}

	public RSId getRsId(Type type)
	{
		var builder = new RSIdBuilder(type);

		builder.setName(getProfile().getName().getBytes())
				.setProfile((getProfile()))
				.setLocationId(getLocationId())
				.setPgpFingerprint(getProfile().getProfileFingerprint().getBytes());

		// Sort the connections with the most recently connected address first
		getConnections().stream()
				.sorted(Comparator.comparing(Connection::getLastConnected, Comparator.nullsFirst(Comparator.naturalOrder())).reversed())
				.forEach(builder::addLocator);

		return builder.build();
	}

	/**
	 * Add a connection while avoiding duplicates.
	 *
	 * @param connection the connection to add
	 */
	public void addConnection(Connection connection)
	{
		var connectionAlreadyExists = getConnections().stream()
				.filter(existingConnection -> existingConnection.equals(connection))
				.findFirst();

		if (connectionAlreadyExists.isEmpty())
		{
			connection.setLocation(this);
			getConnections().add(connection);
		}
	}

	public Profile getProfile()
	{
		return profile;
	}

	public void setProfile(Profile profile)
	{
		this.profile = profile;
	}

	public long getId()
	{
		return id;
	}

	void setId(long id)
	{
		this.id = id;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	@XmlAttribute
	public String getName()
	{
		return name;
	}

	public boolean isConnected()
	{
		return connected;
	}

	public void setConnected(boolean connected)
	{
		this.connected = connected;
		setLastConnected(Instant.now());
	}

	public boolean isDiscoverable()
	{
		return discoverable;
	}

	public void setDiscoverable(boolean discoverable)
	{
		this.discoverable = discoverable;
	}

	public boolean isDht()
	{
		return dht;
	}

	public void setDht(boolean dht)
	{
		this.dht = dht;
	}

	public String getVersion()
	{
		return version;
	}

	public void setVersion(String version)
	{
		this.version = version;
	}

	public NetMode getNetMode()
	{
		return netMode;
	}

	public void setNetMode(NetMode netMode)
	{
		this.netMode = netMode;
	}

	public void setLocationId(LocationId locationId)
	{
		this.locationId = locationId;
	}

	@XmlAttribute
	@XmlJavaTypeAdapter(LocationIdXmlAdapter.class)
	public LocationId getLocationId()
	{
		return locationId;
	}

	public List<Connection> getConnections()
	{
		return connections;
	}

	public List<GxsClientUpdate> getClientUpdates()
	{
		return clientUpdates;
	}

	public Instant getLastConnected()
	{
		return lastConnected;
	}

	public void setLastConnected(Instant lastConnected)
	{
		this.lastConnected = lastConnected;
	}

	public boolean isOwn()
	{
		return id == OWN_LOCATION_ID;
	}

	/**
	 * Returns the best connection. Prefers connections most recently connected to and prefers the LAN
	 * address if the external address is the same as the host.
	 *
	 * @param index     index of the connection, is supposed to always increment so that a different connection is returned
	 * @param ipToAvoid the IP to put last
	 * @return a connection or empty if none
	 */
	public Stream<Connection> getBestConnection(int index, String ipToAvoid)
	{
		if (connections.isEmpty())
		{
			return Stream.empty();
		}
		var connectionsSortedByMostReliable = connections.stream()
				.sorted(comparing(Location::getConnectionAsIpv4, new OwnIpComparator<>(ipToAvoid))
						.thenComparing(Connection::getLastConnected, nullsLast(reverseOrder())))
				.toList();

		return Stream.of(connectionsSortedByMostReliable.get(index % connectionsSortedByMostReliable.size()));
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o)
		{
			return true;
		}
		if (o == null || getClass() != o.getClass())
		{
			return false;
		}
		var location = (Location) o;
		return locationId.equals(location.locationId);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(locationId);
	}

	@Override
	public String toString()
	{
		return locationId.toString();
	}

	private static String getConnectionAsIpv4(Connection connection)
	{
		if (connection.getType() == PeerAddress.Type.IPV4)
		{
			return connection.getIp();
		}
		return "";
	}

	/**
	 * Comparator that puts connections with our own IP as last.
	 *
	 * @param <T>
	 */
	static final class OwnIpComparator<T> implements Comparator<T>
	{
		private final String ipToAvoid;

		OwnIpComparator(String ipToAvoid)
		{
			this.ipToAvoid = ipToAvoid;
		}

		@Override
		public int compare(T o1, T o2)
		{
			if (o1.equals(ipToAvoid))
			{
				if (o2.equals(ipToAvoid))
				{
					return 0;
				}
				else
				{
					return 1;
				}
			}
			else if (o2.equals(ipToAvoid))
			{
				return -1;
			}
			else
			{
				return 0;
			}
		}
	}
}
