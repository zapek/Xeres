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

package io.xeres.app;

import io.xeres.app.application.environment.*;
import io.xeres.app.util.ProfileFileUtils;
import io.xeres.common.mui.MUI;
import io.xeres.common.properties.StartupProperties;
import io.xeres.common.util.ScrambledString;
import io.xeres.ui.UiStarter;
import io.xeres.ui.support.splash.SplashService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static io.xeres.common.properties.StartupProperties.Property.UI;

@SpringBootApplication(scanBasePackageClasses = {io.xeres.app.XeresApplication.class, io.xeres.ui.UiStarter.class})
public class XeresApplication
{
	private static final Logger log = LoggerFactory.getLogger(XeresApplication.class);

	// Spring Boot requires main to be static, always
	static void main(String[] args)
	{
		Thread.setDefaultUncaughtExceptionHandler(XeresApplication::handleException);

		DefaultProperties.setDefaults();

		Cloud.checkIfRunningOnCloud();
		HostVariable.parse();
		CommandArgument.parse(args);
		DataDirLocator.init();
		LocalPortFinder.ensureFreePort();
		var databaseEncryptor = DatabaseEncryptor.getInstance();
		databaseEncryptor.init(DataDirLocator.getDataDir());

		if (StartupProperties.getBoolean(UI, true))
		{
			log.info("gui mode");
			if (ProfileFileUtils.hasSecretProfileKey())
			{
				if (!databaseEncryptor.readAutoLogin())
				{
					SplashService.save(); // The password window will close the splash screen so we need to recreate one
					var passwordResponse = MUI.getInstance().requestPassword();
					if (passwordResponse == null)
					{
						return;
					}
					SplashService.restore();
					databaseEncryptor.setPassphrase(passwordResponse.password());
					if (passwordResponse.autoLogin())
					{
						databaseEncryptor.enableAutoLogin();
					}
				}
			}
			UiStarter.start(XeresApplication.class, args); // this starts spring as well
		}
		else
		{
			log.info("no gui mode");
			if (ProfileFileUtils.hasSecretProfileKey())
			{
				var passphrase = getHeadlessPassword();
				if (passphrase == null)
				{
					return;
				}
				databaseEncryptor.setPassphrase(passphrase);
			}
			SpringApplication.run(XeresApplication.class, args);
		}
	}

	private static ScrambledString getHeadlessPassword()
	{
		var envPassword = System.getenv("XERES_PROFILE_PASSWORD");
		if (envPassword != null)
		{
			return new ScrambledString(envPassword);
		}

		var console = System.console();
		if (console != null)
		{
			var passphrase = console.readPassword("Password: ");
			if (passphrase != null)
			{
				try
				{
					return new ScrambledString(passphrase);
				}
				finally
				{
					ScrambledString.clear(passphrase);
				}
			}
		}
		else
		{
			CommandArgument.portableOutput("No console available to get the password");
		}
		return null;
	}

	private static void handleException(Thread thread, Throwable throwable)
	{
		MUI.getInstance().showError((Exception) throwable);
		System.exit(1);
	}
}
