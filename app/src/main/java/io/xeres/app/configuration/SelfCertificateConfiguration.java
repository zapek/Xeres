/*
 * Copyright (c) 2024 by David Gerber - https://zapek.com
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

package io.xeres.app.configuration;

import io.xeres.app.crypto.rsa.RSA;
import org.apache.catalina.connector.Connector;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.time.Instant;
import java.util.Date;

/**
 * Strongly inspired from <a href="https://valb3r.github.io/letsencrypt-helper/">let's encrypt helper</a> by Valentyn Berezin.
 */
@Configuration
@ConditionalOnExpression("'${server.ssl.enabled}' == 'true' && '${spring.main.web-application-type}' != 'none'")
public class SelfCertificateConfiguration implements TomcatConnectorCustomizer
{
	private static final Logger log = LoggerFactory.getLogger(SelfCertificateConfiguration.class);

	private static final int KEY_SIZE = 3072;

	private final ServerProperties serverProperties;

	public SelfCertificateConfiguration(ServerProperties serverProperties, DataDirConfiguration dataDirConfiguration)
	{
		this.serverProperties = serverProperties;
		if (dataDirConfiguration.getDataDir() == null) // Ignore for tests...
		{
			return;
		}

		serverProperties.getSsl().setKeyStore("file:" + Path.of(dataDirConfiguration.getDataDir(), "keystore.pfx").toAbsolutePath());

		createKeystoreIfNeeded();
	}

	private void createKeystoreIfNeeded()
	{
		var keystoreFile = getKeystoreFile();
		if (keystoreFile.exists())
		{
			log.debug("Keystore exists: {}", keystoreFile.getAbsolutePath());
			return;
		}

		log.info("Creating self-signed certificate for HTTPS access...");
		var keystore = createKeystoreWithSelfSignedCertificate();
		saveKeystore(keystoreFile, keystore);
		log.info("Created keystore {}", keystoreFile.getAbsolutePath());
	}

	private File getKeystoreFile()
	{
		return new File(parseCertificateKeystoreFilePath(serverProperties.getSsl().getKeyStore()));
	}

	private String parseCertificateKeystoreFilePath(String path)
	{
		return path.replace("file://", "").replace("file:", "");
	}

	private KeyStore createKeystoreWithSelfSignedCertificate()
	{
		try
		{
			var domainKey = RSA.generateKeys(KEY_SIZE);
			var newKeystore = KeyStore.getInstance(serverProperties.getSsl().getKeyStoreType());
			newKeystore.load(null, null);
			var signedDomain = selfSign(domainKey, Instant.EPOCH, Instant.EPOCH);
			newKeystore.setKeyEntry(serverProperties.getSsl().getKeyAlias(), domainKey.getPrivate(), keyPassword().toCharArray(), new Certificate[]{signedDomain});
			return newKeystore;
		}
		catch (KeyStoreException | CertificateException | IOException | NoSuchAlgorithmException e)
		{
			throw new RuntimeException(e);
		}
	}

	private Certificate selfSign(KeyPair keyPair, Instant notBefore, Instant notAfter)
	{
		var dnName = new X500Name("CN=Xeres");
		var serialNumber = BigInteger.valueOf(Instant.now().toEpochMilli());
		var subjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded());
		var builder = new X509v3CertificateBuilder(
				dnName,
				serialNumber,
				Date.from(notBefore),
				Date.from(notAfter),
				dnName,
				subjectPublicKeyInfo
		);
		try
		{
			var contentSigner = new JcaContentSignerBuilder("SHA256WithRSA").build(keyPair.getPrivate());
			var certificateHolder = builder.build(contentSigner);
			return new JcaX509CertificateConverter().getCertificate(certificateHolder);
		}
		catch (CertificateException | OperatorCreationException e)
		{
			throw new RuntimeException(e);
		}
	}

	private String keyPassword()
	{
		return serverProperties.getSsl().getKeyPassword() != null ? serverProperties.getSsl().getKeyPassword() : serverProperties.getSsl().getKeyStorePassword();
	}

	private void saveKeystore(File keystoreFile, KeyStore keystore)
	{
		try (var out = Files.newOutputStream(keystoreFile.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING))
		{
			keystore.store(out, serverProperties.getSsl().getKeyStorePassword().toCharArray());
		}
		catch (CertificateException | IOException | NoSuchAlgorithmException | KeyStoreException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public void customize(Connector connector)
	{
		// This is needed so that our configuration is called early, before Tomcat is initialized.
	}
}
