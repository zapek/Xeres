/*
 * Copyright (c) 2023 by David Gerber - https://zapek.com
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

import com.sun.jna.*;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import io.micrometer.common.util.StringUtils;

import java.util.List;
import java.util.Optional;

/**
 * Class to handle the color of window borders for dark themes. Currently only works on Windows.
 */
public final class UiBorders
{
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

	public static void setDarkModeAll(boolean value)
	{
		findAllWindowHandle().forEach(windowHandle -> dwmSetBooleanValue(windowHandle, DwmAttribute.DWMWA_USE_IMMERSIVE_DARK_MODE, value));
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
}
