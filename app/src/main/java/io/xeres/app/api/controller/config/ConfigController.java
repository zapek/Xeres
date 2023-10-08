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

package io.xeres.app.api.controller.config;

import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.xeres.app.api.exception.InternalServerErrorException;
import io.xeres.app.database.model.connection.Connection;
import io.xeres.app.net.protocol.PeerAddress;
import io.xeres.app.service.CapabilityService;
import io.xeres.app.service.LocationService;
import io.xeres.app.service.NetworkService;
import io.xeres.app.service.ProfileService;
import io.xeres.app.service.backup.BackupService;
import io.xeres.app.xrs.service.identity.IdentityRsService;
import io.xeres.common.rest.Error;
import io.xeres.common.rest.config.*;
import jakarta.validation.Valid;
import jakarta.xml.bind.JAXBException;
import org.bouncycastle.openpgp.PGPException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.Optional;
import java.util.Set;

import static io.xeres.app.service.ResourceCreationState.ALREADY_EXISTS;
import static io.xeres.app.service.ResourceCreationState.FAILED;
import static io.xeres.common.rest.PathConfig.*;

@Tag(name = "Configuration", description = "Runtime general configuration", externalDocs = @ExternalDocumentation(url = "https://xeres.io/docs/api/config", description = "Configuration documentation"))
@RestController
@RequestMapping(value = CONFIG_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
public class ConfigController
{
	private static final Logger log = LoggerFactory.getLogger(ConfigController.class);

	private final ProfileService profileService;
	private final LocationService locationService;
	private final IdentityRsService identityRsService;
	private final CapabilityService capabilityService;
	private final BackupService backupService;
	private final NetworkService networkService;

	public ConfigController(ProfileService profileService, LocationService locationService, IdentityRsService identityRsService, CapabilityService capabilityService, BackupService backupService, NetworkService networkService)
	{
		this.profileService = profileService;
		this.locationService = locationService;
		this.identityRsService = identityRsService;
		this.capabilityService = capabilityService;
		this.backupService = backupService;
		this.networkService = networkService;
	}

	@PostMapping("/profile")
	@Operation(summary = "Create own profile")
	@ApiResponse(responseCode = "200", description = "Profile already exists")
	@ApiResponse(responseCode = "201", description = "Profile created successfully", headers = @Header(name = "Location", description = "The location of the created profile", schema = @Schema(type = "string")))
	@ApiResponse(responseCode = "422", description = "Profile entity cannot be processed", content = @Content(schema = @Schema(implementation = Error.class)))
	@ApiResponse(responseCode = "500", description = "Serious error", content = @Content(schema = @Schema(implementation = Error.class)))
	public ResponseEntity<Void> createOwnProfile(@Valid @RequestBody OwnProfileRequest ownProfileRequest)
	{
		var name = ownProfileRequest.name();
		log.debug("Processing creation of Profile {}", name);

		var status = profileService.generateProfileKeys(name);

		if (status == FAILED)
		{
			throw new InternalServerErrorException("Failed to generate profile keys");
		}
		networkService.checkReadiness();

		var location = ServletUriComponentsBuilder.fromCurrentRequest().replacePath(PROFILES_PATH + "/{id}").buildAndExpand(1L).toUri();
		return status == ALREADY_EXISTS ? ResponseEntity.ok().build() : ResponseEntity.created(location).build();
	}

	@PostMapping("/location")
	@Operation(summary = "Create own location")
	@ApiResponse(responseCode = "200", description = "Location already exists")
	@ApiResponse(responseCode = "201", description = "Location created successfully", headers = @Header(name = "Location", description = "The location of the created location", schema = @Schema(type = "string")))
	@ApiResponse(responseCode = "500", description = "Serious error", content = @Content(schema = @Schema(implementation = Error.class)))
	public ResponseEntity<Void> createOwnLocation(@Valid @RequestBody OwnLocationRequest ownLocationRequest)
	{
		var name = ownLocationRequest.name();
		log.debug("Processing creation of Location {}", name);

		var status = locationService.generateOwnLocation(name);

		if (status == FAILED)
		{
			throw new InternalServerErrorException("Failed to generate location");
		}
		networkService.checkReadiness();

		var location = ServletUriComponentsBuilder.fromCurrentRequest().replacePath(LOCATIONS_PATH + "/{id}").buildAndExpand(1L).toUri();
		return status == ALREADY_EXISTS ? ResponseEntity.ok().build() : ResponseEntity.created(location).build();
	}

	@PostMapping("/identity")
	@Operation(summary = "Create own identity")
	@ApiResponse(responseCode = "200", description = "Identity already exists")
	@ApiResponse(responseCode = "201", description = "Identity created successfully")
	@ApiResponse(responseCode = "500", description = "Serious error", content = @Content(schema = @Schema(implementation = Error.class)))
	public ResponseEntity<Void> createOwnIdentity(@Valid @RequestBody OwnIdentityRequest ownIdentityRequest)
	{
		var name = ownIdentityRequest.name();
		log.debug("Creating identity {}", name);

		var status = identityRsService.generateOwnIdentity(name, !ownIdentityRequest.anonymous());

		if (status == FAILED)
		{
			throw new InternalServerErrorException("Failed to generate identity");
		}
		networkService.checkReadiness();

		var location = ServletUriComponentsBuilder.fromCurrentRequest().replacePath(IDENTITIES_PATH + "/{id}").buildAndExpand(1L).toUri();
		return status == ALREADY_EXISTS ? ResponseEntity.ok().build() : ResponseEntity.created(location).build();
	}

	@PutMapping("/externalIp")
	@Operation(summary = "Set or update the external IP address and port.", description = "Note that an external IP address is not strictly required if for example the host is on a public IP already.")
	@ApiResponse(responseCode = "201", description = "IP address set successfully", headers = @Header(name = "Location", description = "The location of where to get the IP address", schema = @Schema(type = "string")))
	public ResponseEntity<Void> updateExternalIpAddress(@Valid @RequestBody IpAddressRequest request)
	{
		log.info("External IP address: {}", request);
		var peerAddress = PeerAddress.from(request.ip(), request.port());
		if (peerAddress.isInvalid())
		{
			throw new IllegalArgumentException("IP is invalid");
		}
		if (!peerAddress.isExternal())
		{
			throw new IllegalArgumentException("Wrong external IP address");
		}

		locationService.updateConnection(locationService.findOwnLocation().orElseThrow(), peerAddress);
		var location = ServletUriComponentsBuilder.fromCurrentRequest().build().toUri();
		return ResponseEntity.created(location).build();
	}

	@GetMapping("/externalIp")
	@Operation(summary = "Get the external IP address and port.", description = "Note that an external IP address is not strictly required if for example the host is on a public IP already.")
	@ApiResponse(responseCode = "200", description = "Request successful")
	@ApiResponse(responseCode = "404", description = "No location or no external IP address", content = @Content(schema = @Schema(implementation = Error.class)))
	public IpAddressResponse getExternalIpAddress()
	{
		var connection = locationService.findOwnLocation().orElseThrow()
				.getConnections()
				.stream()
				.filter(Connection::isExternal)
				.findFirst().orElseThrow();

		return new IpAddressResponse(connection.getIp(), connection.getPort());
	}

	@GetMapping("/internalIp")
	@Operation(summary = "Get the internal IP address and port.")
	@ApiResponse(responseCode = "200", description = "Request successful")
	@ApiResponse(responseCode = "404", description = "No location or no internal IP address", content = @Content(schema = @Schema(implementation = Error.class)))
	public IpAddressResponse getInternalIpAddress()
	{
		return new IpAddressResponse(Optional.ofNullable(networkService.getLocalIpAddress()).orElseThrow(), networkService.getPort());
	}

	@GetMapping("/hostname")
	@Operation(summary = "Get the machine's hostname.")
	@ApiResponse(responseCode = "200", description = "Request successful")
	@ApiResponse(responseCode = "404", description = "No hostname (host configuration problem)", content = @Content(schema = @Schema(implementation = Error.class)))
	public HostnameResponse getHostname() throws UnknownHostException
	{
		return new HostnameResponse(locationService.getHostname());
	}

	@GetMapping("/username")
	@Operation(summary = "Get the OS session's  username.")
	@ApiResponse(responseCode = "200", description = "Request successful")
	@ApiResponse(responseCode = "404", description = "No username (no user session)", content = @Content(schema = @Schema(implementation = Error.class)))
	public UsernameResponse getUsername()
	{
		return new UsernameResponse(locationService.getUsername());
	}

	@GetMapping("/capabilities")
	@Operation(summary = "Get the system's capabilities.")
	@ApiResponse(responseCode = "200", description = "Request successful")
	public Set<String> getCapabilities()
	{
		return capabilityService.getCapabilities();
	}

	@GetMapping(value = "/export", produces = MediaType.APPLICATION_XML_VALUE)
	@Operation(summary = "Export a minimal configuration")
	@ApiResponse(responseCode = "200", description = "Request successful")
	public ResponseEntity<byte[]> getBackup() throws JAXBException
	{
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"xeres_backup.xml\"")
				.body(backupService.backup());
	}

	@PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "Import a minimal configuration")
	@ApiResponse(responseCode = "200", description = "Request successful")
	public ResponseEntity<Void> restoreFromBackup(@RequestBody MultipartFile file) throws JAXBException, IOException, InvalidKeyException, CertificateException, NoSuchAlgorithmException, InvalidKeySpecException, PGPException
	{
		backupService.restore(file);
		networkService.checkReadiness();

		return ResponseEntity.ok().build();
	}
}
