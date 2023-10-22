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

package io.xeres.app;

import io.xeres.app.application.environment.CommandArgument;
import io.xeres.app.application.environment.HostVariable;
import io.xeres.app.application.environment.LocalPortFinder;
import io.xeres.common.properties.StartupProperties;
import io.xeres.ui.UiStarter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static io.xeres.app.application.environment.Cloud.isRunningOnCloud;
import static io.xeres.common.properties.StartupProperties.Property.UI;

@SpringBootApplication(scanBasePackageClasses = {io.xeres.app.XeresApplication.class, io.xeres.ui.UiStarter.class})
public class XeresApplication
{
	private static final Logger log = LoggerFactory.getLogger(XeresApplication.class);

	public static void main(String[] args)
	{
		HostVariable.parse();
		CommandArgument.parse(args);
		LocalPortFinder.ensureFreePort();

		if (isRunningOnCloud() || !StartupProperties.getBoolean(UI, true))
		{
			log.info("no gui mode");
			SpringApplication.run(XeresApplication.class, args);
		}
		else
		{
			log.info("gui mode");
			UiStarter.start(XeresApplication.class, args); // this starts spring as well
		}
	}

	public static boolean isRemoteUiClient()
	{
		return "none".equals(System.getProperty("spring.main.web-application-type"));
	}
}
