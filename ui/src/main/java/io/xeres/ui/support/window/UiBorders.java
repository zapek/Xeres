/*
 * Copyright (c) 2023-2025 by David Gerber - https://zapek.com
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

package io.xeres.ui.support.window;

import com.sun.javafx.stage.WindowHelper;
import com.sun.jna.*;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import io.micrometer.common.util.StringUtils;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;


/**
 * Class to handle the color of window borders for dark themes. Currently only works on Windows.
 */
public final class UiBorders
{
	private static final Logger log = LoggerFactory.getLogger(UiBorders.class);

	private static final BorderCalculationMethod BORDER_CALCULATION_METHOD = BorderCalculationMethod.LOCAL_BOUNDS;

	private enum BorderCalculationMethod
	{
		LOCAL_BOUNDS,
		INSETS
	}

	private UiBorders()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	/**
	 * DWM attributes, see: <a href="https://learn.microsoft.com/en-us/windows/win32/api/dwmapi/ne-dwmapi-dwmwindowattribute">the Microsoft API docs</a>
	 */
	private enum DwmAttribute
	{
		DWMWA_USE_IMMERSIVE_DARK_MODE(20);

		public final int value;

		DwmAttribute(int value)
		{
			this.value = value;
		}
	}

	public static final class WindowHandle
	{
		private final WinDef.HWND value;

		private WindowHandle(WinDef.HWND hwnd)
		{
			value = hwnd;
		}
	}

	private interface DwmSupport extends Library
	{
		DwmSupport INSTANCE = Platform.getOSType() == Platform.WINDOWS ? Native.load("dwmapi", DwmSupport.class) : null;

		WinNT.HRESULT DwmSetWindowAttribute(
				WinDef.HWND hwnd,
				int dwAttribute,
				PointerType pvAttribute,
				int cbAttribute
		);
	}

	private static void dwmSetBooleanValue(WindowHandle handle, DwmAttribute attribute, boolean value)
	{
		if (handle != null)
		{
			DwmSupport.INSTANCE.DwmSetWindowAttribute(
					handle.value,
					attribute.value,
					new WinDef.BOOLByReference(new WinDef.BOOL(value)),
					WinDef.BOOL.SIZE
			);
		}
	}

	public static void setDarkModeOnOpeningWindow(boolean value)
	{
		findOpeningWindowHandle().ifPresent(windowHandle -> dwmSetBooleanValue(windowHandle, DwmAttribute.DWMWA_USE_IMMERSIVE_DARK_MODE, value));
	}

	public static void setDarkMode(Stage stage, boolean value)
	{
		findWindowHandle(stage).ifPresent(windowHandle -> dwmSetBooleanValue(windowHandle, DwmAttribute.DWMWA_USE_IMMERSIVE_DARK_MODE, value));
	}

	public static void setDarkModeAll(boolean value)
	{
		findAllWindowHandle().forEach(windowHandle -> dwmSetBooleanValue(windowHandle, DwmAttribute.DWMWA_USE_IMMERSIVE_DARK_MODE, value));
	}

	private static Optional<WindowHandle> findWindowHandle(Stage stage)
	{
		var peer = WindowHelper.getPeer(stage);
		if (peer != null)
		{
			return Optional.of(new WindowHandle(new WinDef.HWND(new Pointer(peer.getRawHandle()))));
		}
		return Optional.empty();
	}

	private static Optional<WindowHandle> findOpeningWindowHandle()
	{
		if (Platform.getOSType() != Platform.WINDOWS)
		{
			return Optional.empty();
		}
		var glassWindow = com.sun.glass.ui.Window.getWindows().stream()
				.filter(window -> StringUtils.isEmpty(window.getTitle())) // The opening window has an empty title because it gets initialized later. Yes, this is brittle.
				.findFirst().orElse(null);

		return glassWindow != null ? Optional.of(new WindowHandle(new WinDef.HWND(new Pointer(glassWindow.getNativeWindow())))) : Optional.empty();
	}

	private static List<WindowHandle> findAllWindowHandle()
	{
		if (Platform.getOSType() != Platform.WINDOWS)
		{
			return List.of();
		}

		return com.sun.glass.ui.Window.getWindows().stream()
				.map(window -> new WindowHandle(new WinDef.HWND(new Pointer(window.getNativeWindow()))))
				.toList();
	}

	/**
	 * Calculates the window's decoration sizes (aka the windows borders). To do that, a dummy scene is created and put on an invisible
	 * window, which is opened, the insets are calculated then the window is closed.<br>
	 * This only works if Platform.setExplicitExit() is false.
	 *
	 * @param stage the primary stage
	 */
	public static WindowBorder calculateWindowDecorationSizes(Stage stage)
	{
		if (javafx.application.Platform.isImplicitExit())
		{
			throw new IllegalStateException("implicit exit must not be set for window decoration calculation to work");
		}

		// An dummy scene with an invisible window is created then opened.
		var root = new Region();
		stage.setScene(new Scene(root));
		stage.setOpacity(0.0);
		stage.show();

		var windowBorder = switch (BORDER_CALCULATION_METHOD)
		{
			case LOCAL_BOUNDS ->
			{
				// This method uses local root bounds and screen bounds.
				var parentRoot = stage.getScene().getRoot();
				var localRootBounds = parentRoot.getBoundsInLocal();
				var localRootTopLeft = new Point2D(localRootBounds.getMinX(), localRootBounds.getMinY());
				var localRootTopRight = new Point2D(localRootBounds.getMaxX(), localRootBounds.getMaxY());
				var localRootBottomLeft = new Point2D(localRootBounds.getMinX(), localRootBounds.getMaxY());
				var screenRootTopLeft = parentRoot.localToScreen(localRootTopLeft);
				var screenRootTopRight = parentRoot.localToScreen(localRootTopRight);
				var screenRootBottomLeft = parentRoot.localToScreen(localRootBottomLeft);

				// The invisible stage is closed.
				stage.hide();
				stage.setOpacity(1.0);

				yield new WindowBorder(screenRootTopLeft.getX() - stage.getX(),
						screenRootTopLeft.getY() - stage.getY(),
						stage.getX() + stage.getWidth() - screenRootTopRight.getX(),
						stage.getY() + stage.getHeight() - screenRootBottomLeft.getY());
			}
			case INSETS ->
			{
				var insets = getInsets(stage);

				// Here we close the invisible stage before performing the calculations.
				stage.hide();
				stage.setOpacity(1.0);

				yield new WindowBorder(insets.get().getLeft(),
						insets.get().getTop(),
						insets.get().getRight(),
						insets.get().getBottom());
			}
		};

		log.debug("Calculated window borders: {}", windowBorder);
		return windowBorder.isEmpty() ? WindowBorder.DEFAULT : windowBorder; // Workaround for Linux where border calculation doesn't work somehow
	}

	private static ObjectBinding<Insets> getInsets(Stage stage)
	{
		var scene = stage.getScene();

		return Bindings.createObjectBinding(() -> new Insets(scene.getY(),
						stage.getWidth() - scene.getWidth() - scene.getX(),
						stage.getHeight() - scene.getHeight() - scene.getY(),
						scene.getX()),
				scene.xProperty(),
				scene.yProperty(),
				scene.widthProperty(),
				scene.heightProperty(),
				stage.widthProperty(),
				stage.heightProperty());
	}
}
