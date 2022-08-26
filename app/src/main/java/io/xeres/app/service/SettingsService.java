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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import io.xeres.app.application.events.SettingsChangedEvent;
import io.xeres.app.crypto.x509.X509;
import io.xeres.app.database.model.settings.Settings;
import io.xeres.app.database.model.settings.SettingsMapper;
import io.xeres.app.database.repository.SettingsRepository;
import io.xeres.common.dto.settings.SettingsDTO;
import io.xeres.common.id.LocationId;
import io.xeres.common.protocol.HostPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.security.KeyPair;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Service
@Transactional(readOnly = true)
public class SettingsService
{
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

	public void backup(String file)
	{
		settingsRepository.backupDatabase(file);
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

	public X509Certificate getLocationCertificate()
	{
		try
		{
			return X509.getCertificate(settings.getLocationCertificate());
		}
		catch (CertificateException e)
		{
			throw new IllegalStateException("Certificate is corrupt"); // Can't happen
		}
	}

	public LocationId getLocationId() throws CertificateException
	{
		return X509.getLocationId(getLocationCertificate());
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

	public void setLocalIpAddressAndPort(String localIpAddress, int localPort)
	{
		settings.setLocalIpAddress(localIpAddress);
		settings.setLocalPort(localPort);
	}

	public void setLocalIpAddress(String localIpAddress)
	{
		settings.setLocalIpAddress(localIpAddress);
	}

	public String getLocalIpAddress()
	{
		return settings.getLocalIpAddress();
	}

	public int getLocalPort()
	{
		return settings.getLocalPort();
	}
}
