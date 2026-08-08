/*
 * Copyright (c) 2019-2026 by David Gerber - https://zapek.com
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

import io.xeres.app.application.environment.DataDirLocator;
import io.xeres.app.application.environment.DatabaseEncryptor;
import io.xeres.app.properties.DatabaseProperties;
import io.xeres.app.service.UiBridgeService;
import io.xeres.app.service.UiBridgeService.SplashStatus;
import org.bouncycastle.openpgp.PGPException;
import org.h2.tools.ChangeFileEncryption;
import org.h2.tools.Upgrade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.sql.SQLException;
import java.util.Properties;
import java.util.ResourceBundle;

/**
 * Configuration for the location and options of the database.
 */
@Configuration
public class DataSourceConfiguration
{
	private static final Logger log = LoggerFactory.getLogger(DataSourceConfiguration.class);

	private static final int H2_UPGRADE_FROM_VERSION = 214;
	private static final int H2_UPGRADE_CURRENT_FORMAT = 3;
	private static final String H2_URL_PREFIX = "jdbc:h2:file:";
	private static final String H2_USERNAME = "sa";
	private static final String H2_CIPHER = "AES";
	private static final String H2_DATABASE = "userdata";

	private final DatabaseProperties databaseProperties;
	private final UiBridgeService uiBridgeService;
	private final ResourceBundle bundle;

	public DataSourceConfiguration(DatabaseProperties databaseProperties, UiBridgeService uiBridgeService, ResourceBundle bundle)
	{
		this.databaseProperties = databaseProperties;
		this.uiBridgeService = uiBridgeService;
		this.bundle = bundle;
	}

	@Bean
	@ConditionalOnProperty(prefix = "spring.datasource", name = "url", havingValue = "false", matchIfMissing = true)
	public DataSource getDataSource()
	{
		uiBridgeService.setSplashStatus(SplashStatus.DATABASE);

		var disableTraces = ";TRACE_LEVEL_FILE=0"; // Set to 4 for verbose output using Slf4J

		var dataDir = Path.of(DataDirLocator.getDataDir(), H2_DATABASE).toString();

		log.debug("Using database file: {}", dataDir);

		var dbOpts = ";DB_CLOSE_ON_EXIT=FALSE";

		if (databaseProperties.getCacheSize() != null)
		{
			dbOpts += ";CACHE_SIZE=" + databaseProperties.getCacheSize();
		}

		if (databaseProperties.getMaxCompactTime() != null)
		{
			dbOpts += ";MAX_COMPACT_TIME=" + databaseProperties.getMaxCompactTime();
		}

		dbOpts += ";CIPHER=" + H2_CIPHER;

		var url = H2_URL_PREFIX + dataDir + dbOpts + disableTraces;

		if (!DatabaseEncryptor.getInstance().isEncrypted()) // Encrypted databases cannot be upgraded that way
		{
			var filePath = getDatabasePath(url);
			if (filePath != null)
			{
				upgradeIfNeeded(filePath, url);
				try
				{
					encryptIfNeeded(filePath, DatabaseEncryptor.getInstance().getDatabasePassword());
				}
				catch (IOException | PGPException | InvalidKeyException e)
				{
					throw new IllegalArgumentException("Failed to convert the plain database to an encrypted one", e);
				}
			}
		}

		var builder = DataSourceBuilder
				.create()
				.url(url)
				.username(H2_USERNAME)
				.driverClassName("org.h2.Driver");

		try
		{
			builder.password(new String(DatabaseEncryptor.getInstance().getDatabasePassword()) + " "); // a space separates the database encryption password and the user password. we don't use a user password but the space is still needed
		}
		catch (IOException | InvalidKeyException e)
		{
			throw new IllegalStateException(e);
		}
		catch (PGPException _)
		{
			throw new IllegalArgumentException(bundle.getString("mui.wrong-password"));
		}
		return builder.build();
	}

	private static Path getDatabasePath(String url)
	{
		if (!url.startsWith(H2_URL_PREFIX))
		{
			log.debug("Not an H2 file");
			return null;
		}

		var fileName = url.substring(H2_URL_PREFIX.length(), url.indexOf(";")) + ".mv.db";
		var filePath = Path.of(fileName);

		if (!Files.exists(filePath) || !Files.isRegularFile(filePath, LinkOption.NOFOLLOW_LINKS))
		{
			log.debug("No file present");
			return null;
		}
		return filePath;
	}

	/**
	 * Checks if the database needs to be upgraded.
	 * <p>
	 * Note: doesn't work for encrypted database and the mechanism (uses mvn download) might fail
	 *
	 * @param url the database url
	 */
	private static void upgradeIfNeeded(Path filePath, String url)
	{
		try (var reader = new BufferedReader(new FileReader(filePath.toFile())))
		{
			var header = reader.readLine();
			if (header.contains("format:" + H2_UPGRADE_CURRENT_FORMAT))
			{
				log.debug("No upgrade needed for H2");
				return;
			}
		}
		catch (IOException e)
		{
			throw new RuntimeException("Couldn't read database: " + e.getMessage());
		}

		var properties = new Properties();
		properties.put("USER", H2_USERNAME);
		properties.put("PASSWORD", "");
		try
		{
			Upgrade.upgrade(url, properties, H2_UPGRADE_FROM_VERSION);
		}
		catch (Exception e)
		{
			log.error("Couldn't perform upgrade: {}", e.getMessage(), e);
		}
	}

	private static void encryptIfNeeded(Path filePath, char[] encryptPassword)
	{
		log.info("Converting plain database to encrypted one...");
		try
		{
			ChangeFileEncryption.execute(filePath.getParent().toString(), H2_DATABASE, H2_CIPHER, null, encryptPassword, true);
		}
		catch (SQLException e)
		{
			throw new RuntimeException(e);
		}
	}
}
