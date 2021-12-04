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

package io.xeres.app.xrs.service.discovery;

import io.xeres.app.crypto.pgp.PGP;
import io.xeres.app.database.DatabaseSession;
import io.xeres.app.database.DatabaseSessionManager;
import io.xeres.app.database.model.connection.Connection;
import io.xeres.app.database.model.identity.Identity;
import io.xeres.app.database.model.location.Location;
import io.xeres.app.database.model.profile.Profile;
import io.xeres.app.net.peer.PeerConnection;
import io.xeres.app.net.peer.PeerConnectionManager;
import io.xeres.app.net.protocol.PeerAddress;
import io.xeres.app.service.IdentityService;
import io.xeres.app.service.LocationService;
import io.xeres.app.service.ProfileService;
import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.service.RsService;
import io.xeres.app.xrs.service.RsServiceInitPriority;
import io.xeres.app.xrs.service.RsServiceType;
import io.xeres.app.xrs.service.discovery.item.DiscoveryContactItem;
import io.xeres.app.xrs.service.discovery.item.DiscoveryIdentityListItem;
import io.xeres.app.xrs.service.discovery.item.DiscoveryPgpKeyItem;
import io.xeres.app.xrs.service.discovery.item.DiscoveryPgpListItem;
import io.xeres.app.xrs.service.gxsid.GxsIdService;
import io.xeres.common.id.Id;
import io.xeres.common.id.ProfileFingerprint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.SocketAddress;
import java.security.InvalidKeyException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static io.xeres.app.xrs.service.RsServiceType.GOSSIP_DISCOVERY;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toSet;

@Component
public class DiscoveryService extends RsService
{
	private static final Logger log = LoggerFactory.getLogger(DiscoveryService.class);

	private final ProfileService profileService;
	private final LocationService locationService;
	private final IdentityService identityService;
	private final GxsIdService gxsIdService;
	private final BuildProperties buildProperties;
	private final DatabaseSessionManager databaseSessionManager;

	public DiscoveryService(Environment environment, PeerConnectionManager peerConnectionManager, ProfileService profileService, LocationService locationService, IdentityService identityService, GxsIdService gxsIdService, BuildProperties buildProperties, DatabaseSessionManager databaseSessionManager)
	{
		super(environment, peerConnectionManager);
		this.profileService = profileService;
		this.locationService = locationService;
		this.identityService = identityService;
		this.gxsIdService = gxsIdService;
		this.buildProperties = buildProperties;
		this.databaseSessionManager = databaseSessionManager;
	}

	@Override
	public RsServiceType getServiceType()
	{
		return GOSSIP_DISCOVERY;
	}

	@Override
	public Map<Class<? extends Item>, Integer> getSupportedItems()
	{
		return Map.of(
				DiscoveryPgpListItem.class, 1,
				DiscoveryContactItem.class, 5,
				DiscoveryIdentityListItem.class, 6,
				DiscoveryPgpKeyItem.class, 9
		);
	}

	@Override
	public RsServiceInitPriority getInitPriority()
	{
		return RsServiceInitPriority.NORMAL;
	}

	@Override
	public void initialize(PeerConnection peerConnection)
	{
		peerConnection.schedule(
				() -> sendOwnContactAndIdentities(peerConnection)
				, 0,
				TimeUnit.SECONDS
		);
	}

	private void sendOwnContactAndIdentities(PeerConnection peerConnection)
	{
		try (var session = new DatabaseSession(databaseSessionManager))
		{
			var ownLocation = locationService.findOwnLocation().orElseThrow();
			sendContact(peerConnection, ownLocation);
			sendIdentity(peerConnection, identityService.getOwnIdentity()); // XXX: in the future we will have several identities, just get the signed ones here
			// XXX: also send our own other locations, if any (ie. laptop, etc...). XXX: this should be already done in the current code but check. it is done when the peer sends us his list of friends and has us in it
		}
	}

	private void sendContact(Location toLocation, Location aboutLocation)
	{
		sendContact(toLocation, aboutLocation, null);
	}

	private void sendContact(PeerConnection peerConnection, Location aboutLocation)
	{
		sendContact(peerConnection.getLocation(), aboutLocation, peerConnection.getCtx().channel().remoteAddress());
	}

	private void sendContact(Location toLocation, Location aboutLocation, SocketAddress toLocationAddress)
	{
		log.debug("Sending contact information of {} to {}", aboutLocation, toLocation);

		var builder = DiscoveryContactItem.builder();

		builder.setPgpIdentifier(aboutLocation.getProfile().getPgpIdentifier());
		builder.setLocationId(aboutLocation.getLocationId());
		builder.setLocationName(aboutLocation.getName());
		if (aboutLocation.isOwn())
		{
			builder.setVersion(buildProperties.getName() + " " + buildProperties.getVersion());
		}
		builder.setNetMode(aboutLocation.getNetMode());
		builder.setVsDisc(aboutLocation.isDiscoverable() ? 2 : 0);
		builder.setVsDht(aboutLocation.isDht() ? 2 : 0);
		builder.setLastContact((int) (aboutLocation.getLastConnected() != null ? aboutLocation.getLastConnected().getEpochSecond() : Instant.now().getEpochSecond())); // RS uses Instant.now() XXX: find out if there is any issue with that change. it tells since how long we've been connected
		aboutLocation.getConnections().stream()
				.filter(not(Connection::isExternal))
				.findFirst()
				.ifPresent(connection -> builder.setLocalAddressV4(PeerAddress.fromAddress(connection.getAddress())));
		aboutLocation.getConnections().stream()
				.filter(Connection::isExternal)
				.findFirst()
				.ifPresent(connection -> builder.setExternalAddressV4(PeerAddress.fromAddress(connection.getAddress())));
		if (aboutLocation.equals(toLocation) && toLocationAddress != null)
		{
			// Tell the peer about how we see its IP address
			builder.setCurrentConnectAddress(PeerAddress.fromSocketAddress(toLocationAddress));
		}
		aboutLocation.getConnections().stream()
				.filter(connection -> connection.getType() == PeerAddress.Type.HOSTNAME)
				.findFirst()
				.ifPresent(connection -> builder.setHostname(connection.getHostname()));
		writeItem(toLocation, builder.build());
	}

	private void sendIdentity(PeerConnection peerConnection, Identity identity)
	{
		log.debug("Sending our own identity {} to {}", identity, peerConnection);

		writeItem(peerConnection, new DiscoveryIdentityListItem(List.of(identity.getGxsIdGroupItem().getGxsId())));
	}

	private void askForPgpKeys(PeerConnection peerConnection, Set<Long> pgpIds)
	{
		var pgpListItem = new DiscoveryPgpListItem(DiscoveryPgpListItem.Mode.GET_CERT, pgpIds);
		writeItem(peerConnection, pgpListItem);
	}

	private void sendOwnContacts(PeerConnection peerConnection)
	{
		if (!locationService.findOwnLocation().orElseThrow().isDiscoverable())
		{
			return;
		}

		Set<Long> pgpIds = profileService.getAllDiscoverableProfiles().stream()
				.map(Profile::getPgpIdentifier)
				.collect(toSet());

		log.debug("Sending list of friends...");
		assert !pgpIds.isEmpty();
		writeItem(peerConnection, new DiscoveryPgpListItem(DiscoveryPgpListItem.Mode.FRIENDS, pgpIds));
	}

	@Transactional
	@Override
	public void handleItem(PeerConnection sender, Item item)
	{
		if (item instanceof DiscoveryContactItem discoveryContactItem)
		{
			handleContact(sender, discoveryContactItem);
		}
		else if (item instanceof DiscoveryIdentityListItem discoveryIdentityListItem)
		{
			handleIdentityList(sender, discoveryIdentityListItem);
		}
		else if (item instanceof DiscoveryPgpListItem discoveryPgpListItem)
		{
			handlePgpList(sender, discoveryPgpListItem);
		}
		else if (item instanceof DiscoveryPgpKeyItem discoveryPgpKeyItem)
		{
			handlePgpKey(sender, discoveryPgpKeyItem);
		}
	}

	private void handleContact(PeerConnection peerConnection, DiscoveryContactItem discoveryContactItem)
	{
		var peerLocation = peerConnection.getLocation();
		Optional<Location> contactLocation = locationService.findLocationById(discoveryContactItem.getLocationId());

		if (contactLocation.isPresent())
		{
			if (contactLocation.get().equals(peerLocation))
			{
				// Contact information of the peer
				updateConnectedContact(peerConnection, discoveryContactItem, peerLocation, contactLocation.get());
			}
			else if (contactLocation.get().equals(locationService.findOwnLocation().orElseThrow()))
			{
				// Contact information about ourselves (this can be used to help us find our external IP address
				updateOwnContactLocation(discoveryContactItem);
			}
			else
			{
				// Contact information about our friends
				updateCommonContactLocation(peerConnection, discoveryContactItem, contactLocation.get());
			}
		}
		else
		{
			// New locations
			addNewContactLocation(discoveryContactItem);
		}
	}

	private void updateConnectedContact(PeerConnection peerConnection, DiscoveryContactItem discoveryContactItem, Location peerLocation, Location contactLocation)
	{
		log.debug("Peer is sending its own location: {}", discoveryContactItem);
		if (discoveryContactItem.getPgpIdentifier() != contactLocation.getProfile().getPgpIdentifier())
		{
			log.error("PGP identifier or peer doesn't match the key we have about him. Ignoring.");
			return;
		}

		var updatedLocation = updateLocation(peerLocation, discoveryContactItem);
		peerConnection.updateLocation(updatedLocation);

		if (peerLocation.getProfile().isPartial())
		{
			// Ask for its PGP public key
			log.debug("Asking for PGP public key of peer");
			askForPgpKeys(peerConnection, Set.of(peerLocation.getProfile().getPgpIdentifier()));
		}
		else
		{
			// Send our friends
			sendOwnContacts(peerConnection);
		}
	}

	private void updateOwnContactLocation(DiscoveryContactItem discoveryContactItem)
	{
		log.debug("Peer is sending our own location: {}", discoveryContactItem);
		// XXX: process the IP in case we don't find our external address and it could help
		// XXX: beware! RS seems to send ipv4 address in the ipv6 structure...
		// XXX: comments also seem to suggest this can be used to check if the connected IP is the same as our external IP (currentConnectedAddress is null/invalid, though (maybe ipv6? grmbl))
	}

	private void updateCommonContactLocation(PeerConnection peerConnection, DiscoveryContactItem discoveryContactItem, Location contactLocation)
	{
		if (contactLocation.getProfile().isAccepted())
		{
			log.debug("Would update friend here");
			var updatedLocation = updateLocation(contactLocation, discoveryContactItem);
			peerConnection.updateLocation(updatedLocation);
		}
	}

	private void addNewContactLocation(DiscoveryContactItem discoveryContactItem)
	{
		log.debug("New location");

		Optional<Profile> profile = profileService.findProfileByPgpIdentifier(discoveryContactItem.getPgpIdentifier());
		if (profile.isPresent())
		{
			if (profile.get().isAccepted())
			{
				// New location of a friend
				var newLocation = Location.createLocation(discoveryContactItem.getLocationName(), profile.get(), discoveryContactItem.getLocationId());
				newLocation = updateLocation(newLocation, discoveryContactItem);
				log.debug("New location of a friend, added: {}", newLocation);
			}
			else
			{
				// Friend of friend, but shouldn't happen because RS only sends common contacts.
				log.debug("New location for profile {} that we have but is not a friend, ignoring...", profile.get());
			}
		}
		else
		{
			// Friend of friend, but shouldn't happen because RS only sends common contacts.
			// We don't have any use for those. RS uses them as potential proxies/relays for the DHT, but I have
			// yet to see this in the wild because it shouldn't happen.
			log.debug("New location for friend of friend {}, ignoring...", log.isDebugEnabled() ? Id.toString(discoveryContactItem.getPgpIdentifier()) : "");
		}
	}

	private Location updateLocation(Location location, DiscoveryContactItem discoveryContactItem)
	{
		var addresses = new ArrayList<PeerAddress>();
		if (discoveryContactItem.getExternalAddressV4() != null)
		{
			addresses.add(discoveryContactItem.getExternalAddressV4());
		}
		if (discoveryContactItem.getLocalAddressV4() != null)
		{
			addresses.add(discoveryContactItem.getLocalAddressV4());
		}
		addresses.addAll(discoveryContactItem.getExternalAddressList());

		return locationService.update(
				location,
				discoveryContactItem.getLocationName(),
				discoveryContactItem.getNetMode(),
				discoveryContactItem.getVersion(),
				discoveryContactItem.getVsDisc() == 2,
				discoveryContactItem.getVsDht() == 2,
				addresses,
				discoveryContactItem.getHostname());
	}

	private void handlePgpList(PeerConnection peerConnection, DiscoveryPgpListItem discoveryPgpListItem)
	{
		var ownLocation = locationService.findOwnLocation().orElseThrow();
		if (!ownLocation.isDiscoverable())
		{
			return;
		}

		if (discoveryPgpListItem.getMode() == DiscoveryPgpListItem.Mode.GET_CERT)
		{
			List<Profile> friends = getMutualFriends(discoveryPgpListItem.getPgpIds());

			friends.forEach(profile -> writeItem(peerConnection, new DiscoveryPgpKeyItem(profile.getPgpIdentifier(), profile.getPgpPublicKeyData()))); // XXX: RS does that slowly it seems... about one key every few seconds
		}
		else if (discoveryPgpListItem.getMode() == DiscoveryPgpListItem.Mode.FRIENDS)
		{
			// The peer sent us his list of friends.
			log.debug("Received peer's list of friends: {}", discoveryPgpListItem);

			// Only ask for the ones we don't already have, including partial profiles
			Set<Long> pgpIds = discoveryPgpListItem.getPgpIds();
			profileService.findAllCompleteProfilesByPgpIdentifiers(pgpIds).stream()
					.map(Profile::getPgpIdentifier)
					.forEach(pgpIds::remove);

			if (!pgpIds.isEmpty())
			{
				askForPgpKeys(peerConnection, pgpIds);
			}

			// Send contact info of all mutual friends with discovery enabled to peer,
			// including the peer itself if it wants to and also our other locations.
			List<Profile> mutualFriends = getMutualFriends(discoveryPgpListItem.getPgpIds());
			List<Location> locationsToSend = mutualFriends.stream()
					.map(Profile::getLocations)
					.flatMap(List::stream)
					.filter(location -> !location.equals(ownLocation)) // own location was sent at beginning
					.toList();

			locationsToSend.forEach(location -> sendContact(peerConnection, location));

			// Inform all our online mutual friends about peer (except itself as we just sent it above).
			locationsToSend.stream()
					.filter(location -> !location.equals(peerConnection.getLocation()) && location.isConnected())
					.forEach(location -> sendContact(location, peerConnection.getLocation()));
		}
	}

	private List<Profile> getMutualFriends(Set<Long> pgpIds)
	{
		return profileService.findAllDiscoverableProfilesByPgpIdentifiers(pgpIds);
	}

	private void handlePgpKey(PeerConnection peerConnection, DiscoveryPgpKeyItem discoveryPgpKeyItem)
	{
		try
		{
			log.debug("Got PGP key for ID {}", log.isDebugEnabled() ? Id.toString(discoveryPgpKeyItem.getPgpIdentifier()) : "");

			var pgpPublicKey = PGP.getPGPPublicKey(discoveryPgpKeyItem.getKeyData());

			if (discoveryPgpKeyItem.getPgpIdentifier() != pgpPublicKey.getKeyID())
			{
				log.warn("PGP key from {} has an ID ({}) which doesn't match the advertised ID {}", peerConnection.getLocation(), pgpPublicKey.getKeyID(), discoveryPgpKeyItem.getPgpIdentifier());
				return;
			}

			var profileFingerprint = new ProfileFingerprint(pgpPublicKey.getFingerprint());
			Optional<Profile> profile = profileService.findProfileByPgpFingerprint(profileFingerprint);
			if (profile.isPresent())
			{
				if (profile.get().isPartial())
				{
					// The PGP key is about a partial profile, thoroughly check if the peer is the partial profile itself
					if (discoveryPgpKeyItem.getPgpIdentifier() == peerConnection.getLocation().getProfile().getPgpIdentifier() // Incoming key PGP id is the one of the remote peer
							&& profile.get().getPgpIdentifier() == peerConnection.getLocation().getProfile().getPgpIdentifier() // ShortInvite PGP ID matches remote peer
							&& profileFingerprint.equals(peerConnection.getLocation().getProfile().getProfileFingerprint())) // Incoming key fingerprint matches remote peer
					{
						// We can save its PGP key and promote it to full profile.
						profile.get().setPgpPublicKeyData(discoveryPgpKeyItem.getKeyData());
						profileService.createOrUpdateProfile(profile.get());

						sendOwnContacts(peerConnection);
					}
				}
				else
				{
					// XXX: check the key and complain if it doesn't match
				}
			}
			else
			{
				// Create a new profile and save the key
				log.debug("Creating new profile for id {}", log.isDebugEnabled() ? Id.toString(discoveryPgpKeyItem.getPgpIdentifier()) : "");
				var newProfile = Profile.createProfile(pgpPublicKey.getUserIDs().next(), pgpPublicKey.getKeyID(), new ProfileFingerprint(pgpPublicKey.getFingerprint()), pgpPublicKey.getEncoded());
				profileService.createOrUpdateProfile(newProfile);
			}
		}
		catch (InvalidKeyException e)
		{
			log.warn("Invalid PGP public key for profile id {}", Id.toString(discoveryPgpKeyItem.getPgpIdentifier()));
		}
		catch (IOException e)
		{
			log.error("Error while reading PGP public key: {}", e.getMessage());
		}
	}

	private void handleIdentityList(PeerConnection peerConnection, DiscoveryIdentityListItem discoveryIdentityListItem)
	{
		log.debug("Got identities from friend: {}, requesting...", discoveryIdentityListItem);
		gxsIdService.requestGxsGroups(peerConnection, discoveryIdentityListItem.getIdentities());
	}
}
