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
import io.xeres.common.location.Availability;
import io.xeres.common.tray.TrayNotificationType;
import io.xeres.ui.client.ConfigClient;
import io.xeres.ui.client.NotificationClient;
import io.xeres.ui.support.notification.NotificationSettings;
import io.xeres.ui.support.sound.SoundService;
import io.xeres.ui.support.window.WindowManager;
import jakarta.annotation.PreDestroy;
import javafx.application.Platform;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.MessageFormat;
import java.util.Objects;
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
	private Image busyImage;

	private String tooltipTitle;

	private final WindowManager windowManager;
	private final NotificationClient notificationClient;
	private final ConfigClient configClient;
	private final NotificationSettings notificationSettings;
	private final SoundService soundService;
	private final ResourceBundle bundle;

	private Disposable availabilityNotificationDisposable;

	public TrayService(WindowManager windowManager, NotificationClient notificationClient, ConfigClient configClient, NotificationSettings notificationSettings, SoundService soundService, ResourceBundle bundle)
	{
		this.windowManager = windowManager;
		this.notificationClient = notificationClient;
		this.configClient = configClient;
		this.notificationSettings = notificationSettings;
		this.soundService = soundService;
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

		var peersMenu = new Menu(bundle.getString("tray.peers") + " >");
		peersMenu.setEnabled(false);

		var statusMenu = new Menu("Status >");
		statusMenu.add(createStateMenuItem(Availability.AVAILABLE));
		statusMenu.add(createStateMenuItem(Availability.BUSY));
		statusMenu.add(createStateMenuItem(Availability.AWAY));

		var exitItem = new MenuItem(bundle.getString("tray.exit"));
		exitItem.addActionListener(e -> exitApplication());

		var popupMenu = new PopupMenu();
		popupMenu.add(launchItem);
		popupMenu.add(peersMenu);
		popupMenu.add(statusMenu);
		popupMenu.addSeparator();
		popupMenu.add(exitItem);

		image = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/image/trayicon.png"));
		eventImage = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/image/trayicon_event.png"));
		busyImage = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/image/trayicon_busy.png"));

		tooltipTitle = title;
		trayIcon = new TrayIcon(image, tooltipTitle, popupMenu);
		trayIcon.setImageAutoSize(true);

		systemTray = SystemTray.getSystemTray();

		trayIcon.addMouseListener(createContextMenuMouseAdapter());

		setStatus(Availability.AVAILABLE);
		setupAvailabilityNotifications();

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

	private MenuItem createStateMenuItem(Availability availability)
	{
		var menuItem = new MenuItem(availability.toString());
		menuItem.addActionListener(e -> configClient.changeAvailability(availability).subscribe());
		return menuItem;
	}

	/**
	 * Exits the application in a clean way.
	 */
	public void exitApplication()
	{
		windowManager.closeAllWindowsAndExit();
	}

	private void setupAvailabilityNotifications()
	{
		availabilityNotificationDisposable = notificationClient.getAvailabilityNotifications()
				.doOnNext(sse -> {
					Objects.requireNonNull(sse.data());

					// Don't chat with oneself
					if (sse.data().locationId() == 1L)
					{
						setStatus(sse.data().availability());
						return;
					}

					if (sse.data().availability() == Availability.OFFLINE)
					{
						removePeer(sse.data().locationId());
					}
					else
					{
						addPeer(sse.data().locationId(), sse.data().profileName(), sse.data().locationName());
					}
				})
				.subscribe();
	}

	private int findPeerItemIndex(Menu peersMenu, long locationId)
	{
		for (var i = 0; i < peersMenu.getItemCount(); i++)
		{
			if (peersMenu.getItem(i).getName().equals(String.valueOf(locationId)))
			{
				return i;
			}
		}
		return -1;
	}

	private int findInsertionPoint(Menu peersMenu, String name)
	{
		for (var i = 0; i < peersMenu.getItemCount(); i++)
		{
			if (peersMenu.getItem(i).getName().compareTo(name) > 0)
			{
				return i;
			}
		}
		return 0;
	}

	private void addPeer(long locationId, String profileName, String locationName)
	{
		var peersMenu = (Menu) trayIcon.getPopupMenu().getItem(1);
		peersMenu.setEnabled(true);

		if (findPeerItemIndex(peersMenu, locationId) != -1)
		{
			return; // Already present
		}

		var peerItem = new MenuItem(String.format("%s (%s)", profileName, locationName));
		peerItem.setName(String.valueOf(locationId));
		peerItem.addActionListener(e -> windowManager.openMessaging(locationId));
		peersMenu.insert(peerItem, findInsertionPoint(peersMenu, peerItem.getName()));
	}

	private void removePeer(long locationId)
	{
		var peersMenu = (Menu) trayIcon.getPopupMenu().getItem(1);

		var index = findPeerItemIndex(peersMenu, locationId);
		if (index != -1)
		{
			peersMenu.remove(index);
		}
		if (peersMenu.getItemCount() == 0)
		{
			peersMenu.setEnabled(false);
		}
	}

	private void setStatus(Availability availability)
	{
		var statusMenu = (Menu) trayIcon.getPopupMenu().getItem(2);

		if (availability != Availability.OFFLINE)
		{
			setStatusItemDisabled(statusMenu, availability.ordinal());
			setBusy(availability == Availability.BUSY);
		}
	}

	private void setStatusItemDisabled(Menu statusMenu, int index)
	{
		for (var i = 0; i < statusMenu.getItemCount(); i++)
		{
			var statusItem = statusMenu.getItem(i);
			statusItem.setEnabled(i != index);
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
							clearEvent();
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
								clearEvent();
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
			if (type == TrayNotificationType.CONNECTION)
			{
				soundService.play(SoundService.SoundType.FRIEND);
			}
		}
	}

	public void setTooltip(String message)
	{
		if (hasSystemTray)
		{
			trayIcon.setToolTip(isNotBlank(message) ? (tooltipTitle + "\n" + message) : tooltipTitle);
		}
	}

	private void setBusy(boolean busy)
	{
		if (busy && trayIcon.getImage() != busyImage)
		{
			trayIcon.setImage(busyImage);
		}
		else if (!busy && trayIcon.getImage() != image)
		{
			trayIcon.setImage(image);
		}
	}

	public void clearEvent()
	{
		if (trayIcon.getImage() != busyImage)
		{
			trayIcon.setImage(image);
		}
	}

	public void setEventIfIconified()
	{
		if (trayIcon.getImage() != busyImage)
		{
			trayIcon.setImage(eventImage);
		}
	}

	private boolean isNotificationAllowed(TrayNotificationType type)
	{
		return switch (type)
		{
			case BROADCAST -> notificationSettings.isBroadcastsEnabled();
			case CONNECTION -> notificationSettings.isConnectionEnabled();
			case DISCOVERY -> notificationSettings.isDiscoveryEnabled();
		};
	}

	@PreDestroy
	private void removeSystemTray()
	{
		if (hasSystemTray)
		{
			availabilityNotificationDisposable.dispose();
			systemTray.remove(trayIcon);
			hasSystemTray = false;
		}
	}
}
