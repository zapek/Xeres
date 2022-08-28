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

package io.xeres.ui.support.tray;

import io.xeres.common.AppName;
import io.xeres.ui.support.window.WindowManager;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

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

	public TrayService(WindowManager windowManager)
	{
		this.windowManager = windowManager;
	}

	public void addSystemTray()
	{
		if (hasSystemTray)
		{
			return;
		}

		if (!SystemTray.isSupported())
		{
			log.error("System tray not supported");
			return;
		}

		// Do not exit the platform when all windows are closed.
		Platform.setImplicitExit(false);

		var exitItem = new MenuItem("Exit");
		exitItem.addActionListener(e ->
		{
			windowManager.closeAllWindows();
			Platform.exit();
		});

		var peersItem = new MenuItem("Peers");
		peersItem.addActionListener(e ->
				windowManager.openPeers());

		var popupMenu = new PopupMenu();
		popupMenu.add(peersItem);
		popupMenu.add(exitItem);

		image = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/image/trayicon.png"));
		eventImage = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/image/trayicon_event.png"));

		trayIcon = new TrayIcon(image, AppName.NAME, popupMenu);
		trayIcon.setImageAutoSize(true);

		systemTray = SystemTray.getSystemTray();

		trayIcon.addMouseListener(new MouseAdapter()
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
						// it's not trivial to recover
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
		});

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
