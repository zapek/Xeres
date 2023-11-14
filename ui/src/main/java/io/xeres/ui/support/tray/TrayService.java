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

	private final WindowManager windowManager;
	private final ResourceBundle bundle;

	public TrayService(WindowManager windowManager, ResourceBundle bundle)
	{
		this.windowManager = windowManager;
		this.bundle = bundle;
	}

	public void addSystemTray()
	{
		if (hasSystemTray)
		{
			return;
		}

		// Only works properly on Windows. On Linux it makes an ugly mess with weird UI and flickering and on
		// MacOS it hangs on exit.
		if (!SystemTray.isSupported() || SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC)
		{
			log.error("System tray not supported on that platform");
			return;
		}

		var launchItem = new MenuItem(MessageFormat.format(bundle.getString("tray.open"), AppName.NAME));
		launchItem.addActionListener(e ->
				windowManager.openMain(null, null, false));

		var peersItem = new MenuItem(bundle.getString("tray.peers"));
		peersItem.addActionListener(e ->
				windowManager.openPeers());

		var separator = new MenuItem("-");

		var exitItem = new MenuItem(bundle.getString("tray.exit"));
		exitItem.addActionListener(e ->
		{
			windowManager.closeAllWindows();
			Platform.exit();
		});

		var popupMenu = new PopupMenu();
		popupMenu.add(launchItem);
		popupMenu.add(peersItem);
		popupMenu.add(separator);
		popupMenu.add(exitItem);

		image = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/image/trayicon.png"));
		eventImage = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/image/trayicon_event.png"));

		trayIcon = new TrayIcon(image, AppName.NAME, popupMenu);
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

	public void showNotification(String message)
	{
		if (hasSystemTray)
		{
			trayIcon.displayMessage(AppName.NAME, message, TrayIcon.MessageType.NONE);
		}
	}

	public void setTooltip(String message)
	{
		if (hasSystemTray)
		{
			trayIcon.setToolTip(isNotBlank(message) ? (AppName.NAME + " - " + message) : AppName.NAME);
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
