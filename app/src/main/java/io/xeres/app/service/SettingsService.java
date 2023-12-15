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

package io.xeres.app.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import io.micrometer.common.util.StringUtils;
import io.xeres.app.application.events.SettingsChangedEvent;
import io.xeres.app.database.model.settings.Settings;
import io.xeres.app.database.model.settings.SettingsMapper;
import io.xeres.app.database.repository.SettingsRepository;
import io.xeres.common.dto.settings.SettingsDTO;
import io.xeres.common.protocol.HostPort;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Objects;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Service
public class SettingsService
{
	private static final Logger log = LoggerFactory.getLogger(SettingsService.class);

	private static final String BACKUP_FILE_PREFIX = "backup_";
	private static final String BACKUP_FILE_EXTENSION = ".zip";
	private static final Pattern BACKUP_FILES = Pattern.compile("^backup_\\d{14}.zip$");
	private static final int BACKUP_FILES_RETENTION = 3;

	private static final DateTimeFormatter backupFileFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
			.withZone(ZoneId.systemDefault());

	private final SettingsRepository settingsRepository;

	private final ApplicationEventPublisher publisher;

	private final ObjectMapper objectMapper;

	private Settings settings;

	public SettingsService(SettingsRepository settingsRepository, ApplicationEventPublisher publisher, ObjectMapper objectMapper)
	{
		this.settingsRepository = settingsRepository;
		this.publisher = publisher;
		this.objectMapper = objectMapper;
	}

	@PostConstruct
	void init()
	{
		settings = settingsRepository.findById((byte) 1).orElseThrow(() -> new IllegalStateException("No setting configuration"));
	}

	/**
	 * Performs a backup of the database.
	 * <p>
	 * The last {@code BACKUP_FILE_RETENTION} files are kept. The rest is deleted. A timestamp is placed within the name of each backup file.
	 *
	 * @param directory the directory in where to place the backup.
	 */
	public void backup(String directory)
	{
		Objects.requireNonNull(directory);

		var backupFile = Path.of(directory, BACKUP_FILE_PREFIX + backupFileFormatter.format(Instant.now()) + BACKUP_FILE_EXTENSION);

		log.info("Doing backup of database to {}", backupFile);
		settingsRepository.backupDatabase(backupFile.toString());
		deleteOldestBackupSiblings(backupFile);
	}

	private void deleteOldestBackupSiblings(Path file)
	{
		try (var pathStream = Files.find(file.getParent(), 1, (path, attributes) -> BACKUP_FILES.matcher(path.getFileName().toString()).matches() && attributes.isRegularFile()))
		{
			pathStream.sorted(Comparator.comparing(path -> path.toFile().lastModified()))
					.sorted(Comparator.reverseOrder())
					.skip(BACKUP_FILES_RETENTION)
					.forEach(this::deleteFile);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	private void deleteFile(Path path)
	{
		try
		{
			Files.delete(path);
		}
		catch (IOException e)
		{
			log.error("Couldn't delete old backup file: {}", path);
		}
	}

	/**
	 * Retrieve the settings. For DTO use only.
	 *
	 * @return the settings as a DTO
	 */
	public SettingsDTO getSettings()
	{
		return SettingsMapper.toDTO(settings);
	}

	@Transactional
	public Settings applyPatchToSettings(JsonPatch jsonPatch)
	{
		try
		{
			var patched = jsonPatch.apply(objectMapper.convertValue(settings, JsonNode.class));
			updateSettings(objectMapper.treeToValue(patched, Settings.class));
		}
		catch (JsonPatchException | JsonProcessingException e)
		{
			throw new IllegalStateException("Failed to patch settings", e);
		}
		return settings;
	}


	private void updateSettings(Settings settings)
	{
		var oldSettings = this.settings;
		this.settings = settings;
		settingsRepository.save(settings);
		publisher.publishEvent(new SettingsChangedEvent(oldSettings, settings));
	}

	// XXX: I think those need 'synchronized' or so... depends how we use them
	@Transactional
	public void saveSecretProfileKey(byte[] privateKeyData)
	{
		settings.setPgpPrivateKeyData(privateKeyData);
		settingsRepository.save(settings);
	}

	public byte[] getSecretProfileKey()
	{
		return settings.getPgpPrivateKeyData();
	}

	@Transactional
	public void saveLocationKeys(KeyPair keyPair)
	{
		settings.setLocationPrivateKeyData(keyPair.getPrivate().getEncoded());
		settings.setLocationPublicKeyData(keyPair.getPublic().getEncoded());
		settingsRepository.save(settings);
	}

	public byte[] getLocationPublicKeyData()
	{
		return settings.getLocationPublicKeyData();
	}

	public byte[] getLocationPrivateKeyData()
	{
		return settings.getLocationPrivateKeyData();
	}

	@Transactional
	public void saveLocationCertificate(byte[] data)
	{
		settings.setLocationCertificate(data);
		settingsRepository.save(settings);
	}

	public byte[] getLocationCertificate()
	{
		return settings.getLocationCertificate();
	}

	public boolean hasOwnLocation()
	{
		return settings.hasLocationCertificate();
	}

	public boolean isOwnProfilePresent()
	{
		return settings.getPgpPrivateKeyData() != null;
	}

	public boolean hasTorSocksConfigured()
	{
		return isNotBlank(settings.getTorSocksHost()) && settings.getTorSocksPort() != 0;
	}

	public HostPort getTorSocksHostPort()
	{
		return new HostPort(settings.getTorSocksHost(), settings.getTorSocksPort());
	}

	public boolean hasI2pSocksConfigured()
	{
		return isNotBlank(settings.getI2pSocksHost()) && settings.getI2pSocksPort() != 0;
	}

	public HostPort getI2pSocksHostPort()
	{
		return new HostPort(settings.getI2pSocksHost(), settings.getI2pSocksPort());
	}

	public boolean isUpnpEnabled()
	{
		return settings.isUpnpEnabled();
	}

	public boolean isBroadcastDiscoveryEnabled()
	{
		return settings.isBroadcastDiscoveryEnabled();
	}

	public boolean isDhtEnabled()
	{
		return settings.isDhtEnabled();
	}

	public int getLocalPort()
	{
		return settings.getLocalPort();
	}

	public void setLocalPort(int port)
	{
		settings.setLocalPort(port);
	}

	public boolean isAutoStartEnabled()
	{
		return settings.isAutoStartEnabled();
	}

	public boolean hasIncomingDirectory()
	{
		return StringUtils.isNotEmpty(settings.getIncomingDirectory());
	}

	public String getIncomingDirectory()
	{
		return settings.getIncomingDirectory();
	}

	public void setIncomingDirectory(String directory)
	{
		settings.setIncomingDirectory(directory);
		settingsRepository.save(settings);
	}
}
