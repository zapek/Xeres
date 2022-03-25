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

package io.xeres.ui;

import io.xeres.common.mui.MinimalUserInterface;
import io.xeres.ui.controller.MainWindowController;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.spring.SpringFxWeaver;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

public class JavaFxApplication extends Application
{
	private ConfigurableApplicationContext springContext;

	private static HostServices hostServices;

	private static Class<?> springApplicationClass;

	private static MainWindowController mainWindowController;

	static void start(Class<?> springApplicationClass, String[] args)
	{
		JavaFxApplication.springApplicationClass = springApplicationClass;
		Application.launch(JavaFxApplication.class, args);
	}

	@Bean
	public FxWeaver fxWeaver(ConfigurableApplicationContext applicationContext)
	{
		return new SpringFxWeaver(applicationContext);
	}

	private boolean isHeadless()
	{
		return getParameters().getUnnamed().contains("no-gui");
	}

	@Override
	public void init()
	{
		try
		{
			springContext = new SpringApplicationBuilder()
					.sources(springApplicationClass)
					.headless(isHeadless()) // JavaFX defaults to true which is not what we want
					.run(getParameters().getRaw().toArray(new String[0]));
		}
		catch (Exception e)
		{
			if (!isHeadless())
			{
				MinimalUserInterface.showError(e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
			}
			throw e;
		}
	}

	@Override
	public void stop()
	{
		springContext.close();
		Platform.exit();
	}

	@Override
	public void start(Stage primaryStage)
	{
		hostServices = getHostServices();

		if (springContext != null)
		{
			mainWindowController = springContext.getBean(MainWindowController.class);
			springContext.publishEvent(new StageReadyEvent(primaryStage));
		}
	}

	public static void openUrl(String url)
	{
		hostServices.showDocument(url);
	}

	public static void addPeer(String rsId)
	{
		mainWindowController.addPeer(rsId);
	}

	public static String getHostnameAndPort()
	{
		return getHostname() + ":" + getControlPort();
	}

	private static String getHostname()
	{
		return System.getProperty("xrs.ui.address", "localhost");
	}

	private static int getControlPort()
	{
		return Integer.parseInt(System.getProperty("xrs.ui.port", "1066"));
	}

	public static String getControlUrl() // XXX: get rid of that thing, just use a properties
	{
		return "http://" + getHostnameAndPort();
	}
}
