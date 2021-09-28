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

import io.xeres.app.crypto.x509.X509;
import io.xeres.app.database.model.prefs.Prefs;
import io.xeres.app.database.repository.PrefsRepository;
import io.xeres.common.id.LocationId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.security.KeyPair;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

@Service
@Transactional(readOnly = true)
public class PrefsService
{
	private final PrefsRepository prefsRepository;

	private Prefs prefs;

	public PrefsService(PrefsRepository prefsRepository)
	{
		this.prefsRepository = prefsRepository;
	}

	@PostConstruct
	void init()
	{
		prefs = prefsRepository.findById((byte) 1).orElseThrow(() -> new IllegalStateException("No setting configuration"));
	}

	@Transactional
	public void save()
	{
		prefsRepository.save(prefs);
	}

	public void backup(String file)
	{
		prefsRepository.backupDatabase(file);
	}

	// XXX: I think those need 'synchronized' or so... depends how we use them
	public void saveSecretProfileKey(byte[] privateKeyData)
	{
		prefs.setPgpPrivateKeyData(privateKeyData);
		save();
	}

	public byte[] getSecretProfileKey()
	{
		return prefs.getPgpPrivateKeyData();
	}

	public void saveLocationKeys(KeyPair keyPair)
	{
		prefs.setLocationPrivateKeyData(keyPair.getPrivate().getEncoded());
		prefs.setLocationPublicKeyData(keyPair.getPublic().getEncoded());
		save();
	}

	public byte[] getLocationPublicKeyData()
	{
		return prefs.getLocationPublicKeyData();
	}

	public byte[] getLocationPrivateKeyData()
	{
		return prefs.getLocationPrivateKeyData();
	}

	public void saveLocationCertificate(byte[] data)
	{
		prefs.setLocationCertificate(data);
		save();
	}

	public X509Certificate getLocationCertificate()
	{
		try
		{
			return X509.getCertificate(prefs.getLocationCertificate());
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
		return prefs.hasLocationCertificate();
	}

	public boolean isOwnProfilePresent()
	{
		return prefs.getPgpPrivateKeyData() != null;
	}
}
