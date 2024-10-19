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

package io.xeres.ui.support.tray;

import io.xeres.common.AppName;
import io.xeres.common.tray.TrayNotificationType;
import io.xeres.ui.properties.UiClientProperties;
import io.xeres.ui.support.window.WindowManager;
import jakarta.annotation.PreDestroy;
import javafx.application.Platform;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.MessageFormat;
import java.util.ResourceBundle;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Service
public class TrayService
{
	private static final Logger log = LoggerFactory.getLogger(TrayService.class);

	private SystemTray systemTray;
	private TrayIcon trayIcon;
	private boolean hasSystemTray;

	private Image image;
	private Image eventImage;

	private String tooltipTitle;

	private final UiClientProperties uiClientProperties;
	private final WindowManager windowManager;
	private final ResourceBundle bundle;

	public TrayService(UiClientProperties uiClientProperties, WindowManager windowManager, ResourceBundle bundle)
	{
		this.uiClientProperties = uiClientProperties;
		this.windowManager = windowManager;
		this.bundle = bundle;
	}

	public void addSystemTray(String title)
	{
		if (hasSystemTray)
		{
			return;
		}

		// Only works properly on Windows. On Linux it depends, Java 21.0.3+ checks if it's supported.
		// On MacOS it hangs on exit.
		if (!SystemTray.isSupported() || SystemUtils.IS_OS_MAC)
		{
			log.error("System tray not supported on that platform");
			return;
		}

		var launchItem = new MenuItem(MessageFormat.format(bundle.getString("tray.open"), AppName.NAME));
		launchItem.addActionListener(e ->
				windowManager.openMain(null, null, false));

		var separator = new MenuItem("-");

		var exitItem = new MenuItem(bundle.getString("tray.exit"));
		exitItem.addActionListener(e -> exitApplication());

		var popupMenu = new PopupMenu();
		popupMenu.add(launchItem);
		popupMenu.add(separator);
		popupMenu.add(exitItem);

		image = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/image/trayicon.png"));
		eventImage = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/image/trayicon_event.png"));

		tooltipTitle = title;
		trayIcon = new TrayIcon(image, tooltipTitle, popupMenu);
		trayIcon.setImageAutoSize(true);

		systemTray = SystemTray.getSystemTray();

		trayIcon.addMouseListener(createContextMenuMouseAdapter());

		try
		{
			systemTray.add(trayIcon);
			hasSystemTray = true;
		}
		catch (AWTException e)
		{
			log.error("Failed to put system tray: {}", e.getMessage(), e);
		}
	}

	/**
	 * Exits the application in a clean way.
	 */
	public void exitApplication()
	{
		windowManager.closeAllWindowsAndExit();
	}

	private MouseAdapter createContextMenuMouseAdapter()
	{
		return new MouseAdapter()
		{
			@Override
			public void mouseReleased(MouseEvent e)
			{
				if (e.getButton() == MouseEvent.BUTTON1)
				{
					Platform.runLater(() ->
					{
						var stage = windowManager.getMainStage();

						// Do not hide an iconified stage otherwise
						// it's not trivial to recover. We don't actually really
						// iconify in the app so this is defensive code.
						if (stage.isIconified())
						{
							stage.setIconified(false);
							setEvent(false);
						}
						else
						{
							if (stage.isShowing())
							{
								stage.hide();
							}
							else
							{
								// Yet another weird workaround that allows to
								// remember the size before iconifying the
								// window.
								stage.setX(stage.getX());
								stage.setY(stage.getY());
								stage.setWidth(stage.getWidth());
								stage.setHeight(stage.getHeight());
								stage.show();
								setEvent(false);
							}
						}
					});
				}
				else
				{
					super.mouseClicked(e);
				}
			}
		};
	}

	public boolean hasSystemTray()
	{
		return hasSystemTray;
	}

	public void showNotification(TrayNotificationType type, String message)
	{
		if (hasSystemTray && isNotificationAllowed(type))
		{
			trayIcon.displayMessage(AppName.NAME, message, TrayIcon.MessageType.NONE);
		}
	}

	public void setTooltip(String message)
	{
		if (hasSystemTray)
		{
			trayIcon.setToolTip(isNotBlank(message) ? (tooltipTitle + "\n" + message) : tooltipTitle);
		}
	}

	public void setEvent(boolean pending)
	{
		trayIcon.setImage(pending ? eventImage : image);
	}

	public void setEventIfIconified()
	{
		trayIcon.setImage(eventImage);
	}

	private boolean isNotificationAllowed(TrayNotificationType type)
	{
		return switch (type)
		{
			case BROADCAST -> true;
			case CONNECTION -> uiClientProperties.isConnectionNotifications();
			case DISCOVERY -> uiClientProperties.isBroadcastDiscoveryNotifications();
		};
	}

	@PreDestroy
	private void removeSystemTray()
	{
		if (hasSystemTray)
		{
			systemTray.remove(trayIcon);
			hasSystemTray = false;
		}
	}
}
