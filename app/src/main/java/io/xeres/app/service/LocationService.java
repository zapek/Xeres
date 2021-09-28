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

package io.xeres.app.service;

import io.xeres.app.application.events.LocationReadyEvent;
import io.xeres.app.crypto.pgp.PGP;
import io.xeres.app.crypto.rsa.RSA;
import io.xeres.app.crypto.rsid.RSSerialVersion;
import io.xeres.app.crypto.x509.X509;
import io.xeres.app.database.model.connection.Connection;
import io.xeres.app.database.model.location.Location;
import io.xeres.app.database.repository.LocationRepository;
import io.xeres.app.net.protocol.PeerAddress;
import io.xeres.common.id.LocationId;
import io.xeres.common.properties.StartupProperties;
import io.xeres.common.protocol.NetMode;
import io.xeres.common.protocol.ip.IP;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import static io.xeres.common.dto.location.LocationConstants.OWN_LOCATION_ID;

@Service
@Transactional(readOnly = true)
public class LocationService
{
	private static final Logger log = LoggerFactory.getLogger(LocationService.class);

	private static final int KEY_SIZE = 3072;

	private static final int CONNECTION_POOL_SIZE = 10; // number of locations to connect at once

	public enum UpdateConnectionStatus
	{
		UPDATED,
		ADDED
	}

	private final PrefsService prefsService;
	private final ProfileService profileService;
	private final LocationRepository locationRepository;
	private final ApplicationEventPublisher publisher;

	private Slice<Location> locations;
	private int pageIndex;
	private int connectionIndex = -1;

	public LocationService(PrefsService prefsService, ProfileService profileService, LocationRepository locationRepository, ApplicationEventPublisher publisher)
	{
		this.prefsService = prefsService;
		this.profileService = profileService;
		this.locationRepository = locationRepository;
		this.publisher = publisher;
	}

	void generateLocationKeys()
	{
		if (prefsService.getLocationPrivateKeyData() != null)
		{
			return;
		}

		log.info("Generating keys, algorithm: RSA, bits: {} ...", KEY_SIZE);

		var keyPair = RSA.generateKeys(KEY_SIZE);

		log.info("Successfully generated key pair");

		prefsService.saveLocationKeys(keyPair);
	}

	void generateLocationCertificate() throws CertificateException, InvalidKeySpecException, NoSuchAlgorithmException, IOException
	{
		if (prefsService.hasOwnLocation())
		{
			return;
		}
		if (!prefsService.isOwnProfilePresent())
		{
			throw new CertificateException("Cannot generate certificate without a profile; Create a profile first");
		}

		log.info("Generating certificate...");

		var x509Certificate = X509.generateCertificate(
				PGP.getPGPSecretKey(prefsService.getSecretProfileKey()),
				RSA.getPublicKey(prefsService.getLocationPublicKeyData()),
				"CN=" + Long.toHexString(profileService.getOwnProfile().getPgpIdentifier()).toUpperCase(Locale.ROOT), // older RS use a random string I think, like 12:34:55:44:4e:44:99:23
				"CN=-",
				new Date(0),
				new Date(0),
				RSSerialVersion.V07_0001.serialNumber()
		);

		log.info("Successfully generated certificate");

		prefsService.saveLocationCertificate(x509Certificate.getEncoded());
	}

	@Transactional
	public void createOwnLocation(String name) throws CertificateException
	{
		if (!prefsService.isOwnProfilePresent())
		{
			throw new CertificateException("Cannot create a location without a profile; Create a profile first");
		}
		var ownProfile = profileService.getOwnProfile();

		if (!ownProfile.getLocations().isEmpty())
		{
			throw new CertificateException("Location already exists");
		}

		String localIpAddress = Optional.ofNullable(IP.getLocalIpAddress()).orElseThrow(() -> new CertificateException("Current host has no IP address. Please configure your network"));

		// Create an IPv4 location (XXX: add other protocols later)
		int localPort = Optional.ofNullable(StartupProperties.getInteger(StartupProperties.Property.SERVER_PORT)).orElseGet(IP::getFreeLocalPort);
		log.info("Using local ip address {} and port {}", localIpAddress, localPort);

		generateLocationKeys();

		try
		{
			generateLocationCertificate();
		}
		catch (InvalidKeySpecException | NoSuchAlgorithmException | IOException e)
		{
			throw new CertificateException("Failed to generate certificate: " + e.getMessage());
		}

		var location = Location.createLocation(name);
		location.setLocationId(prefsService.getLocationId());
		ownProfile.addLocation(location);
		locationRepository.save(location);

		// Send the event asynchronously so that our transaction can complete first
		CompletableFuture.runAsync(() -> publisher.publishEvent(new LocationReadyEvent(localIpAddress, localPort)));
	}

	/**
	 * Find the location.
	 *
	 * @param locationId the SSL identifier
	 * @return the location
	 */
	public Optional<Location> findLocationById(LocationId locationId)
	{
		return locationRepository.findByLocationId(locationId);
	}

	public Optional<Location> findOwnLocation()
	{
		return locationRepository.findById(OWN_LOCATION_ID);
	}

	public Optional<Location> findLocationById(long id)
	{
		return locationRepository.findById(id);
	}

	@Transactional
	public void markAllConnectionsAsDisconnected()
	{
		locationRepository.putAllConnectedToFalse();
	}

	@Transactional
	public void setConnected(Location location, SocketAddress socketAddress)
	{
		updateConnection(location, socketAddress); // XXX: is this the right place? maybe it should be done in discovery service

		location.setConnected(true);
		locationRepository.save(location);
	}

	private void updateConnection(Location location, SocketAddress socketAddress)
	{
		var inetSocketAddress = (InetSocketAddress) socketAddress;

		location.getConnections().stream()
				.filter(conn -> conn.getAddress().split(":")[0].equals(inetSocketAddress.getHostString()))
				.findFirst()
				.ifPresent(connection -> connection.setLastConnected(Instant.now()));
	}

	@Transactional
	public void setDisconnected(Location location)
	{
		location.setConnected(false);
		locationRepository.save(location);
	}

	@Transactional
	public Location update(Location location, String locationName, NetMode netMode, String version, boolean discoverable, boolean dht, List<PeerAddress> peerAddresses, String hostname)
	{
		location.setName(locationName);
		location.setNetMode(netMode);
		location.setVersion(version);
		location.setDiscoverable(discoverable);
		location.setDht(dht);
		peerAddresses.forEach(peerAddress -> updateConnection(location, peerAddress));
		// XXX: missing hostname. where's the hostname support?! is it in the connection? I don't think that's the right place for it... should be the location
		return locationRepository.save(location);
	}

	public List<Connection> getConnectionsToConnectTo()
	{
		locations = locationRepository.findAllByConnectedFalse(PageRequest.of(getPageIndex(), getPageSize(), Sort.by("lastConnected").descending())); // XXX: check if the sorting works

		return locations.stream()
				.filter(Predicate.not(Location::isOwn))
				.flatMap(location -> location.getBestConnection(getConnectionIndex()))
				.limit(CONNECTION_POOL_SIZE)
				.toList();
	}

	public List<Location> getConnectedLocations()
	{
		return locationRepository.findAllByConnectedTrue();
	}

	@Transactional
	public UpdateConnectionStatus updateConnection(Location location, PeerAddress peerAddress)
	{
		var updated = false;

		if (location.isOwn())
		{
			for (Connection connection : location.getConnections())
			{
				updated = updateConnectionIfSameType(peerAddress, connection);
				if (updated)
				{
					break;
				}
			}

		}
		else
		{
			for (Connection connection : location.getConnections())
			{
				if (peerAddress.getType() == connection.getType()
						&& peerAddress.getAddress().orElseThrow().equals(connection.getAddress()))
				{
					updated = true;
					break;
				}
			}

		}
		if (!updated)
		{
			location.addConnection(Connection.from(peerAddress));
		}
		locationRepository.save(location);
		return updated ? UpdateConnectionStatus.UPDATED : UpdateConnectionStatus.ADDED;
	}

	public String getHostname() throws UnknownHostException
	{
		return InetAddress.getLocalHost().getHostName();
	}

	public String getUsername()
	{
		String username = System.getProperty("user.name");
		if (StringUtils.isEmpty(username))
		{
			throw new NoSuchElementException("No logged in username");
		}
		return username;
	}

	private boolean updateConnectionIfSameType(PeerAddress from, Connection to)
	{
		if ((from.isExternal() && to.isExternal())
				|| (!from.isExternal() && !to.isExternal()))
		{
			to.setAddress(from.getAddress().orElseThrow());
			return true;
		}
		return false;
	}

	private int getPageIndex()
	{
		if (locations == null || locations.isLast())
		{
			pageIndex = 0;
			connectionIndex++;
		}
		else
		{
			pageIndex++;
		}
		return pageIndex;
	}

	private int getPageSize()
	{
		return CONNECTION_POOL_SIZE; // XXX: make it dynamic depending on the connection speed and reliability
	}

	private int getConnectionIndex()
	{
		return connectionIndex;
	}
}
