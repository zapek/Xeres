/*
 * Copyright (c) 2019-2022 by David Gerber - https://zapek.com
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

package io.xeres.app.xrs.service.status;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Structure;
import com.sun.jna.platform.unix.X11;

class GetIdleTimeLinux implements GetIdleTime
{
	@SuppressWarnings("unused")
	private class XScreenSaverInfo extends Structure
	{
		public X11.Window window;
		public int state;
		public int kind;
		public NativeLong til_or_since;
		public NativeLong idle;
		public NativeLong event_mask;
	}

	private interface Xss extends Library
	{
		Xss INSTANCE = Native.load("Xss", Xss.class);

		@SuppressWarnings("UnusedReturnValue")
		int XScreenSaverQueryInfo(X11.Display display, X11.Drawable drawable, XScreenSaverInfo xScreenSaverInfo);
	}


	@Override
	public int getIdleTime()
	{
		X11.Display display = null;
		X11.Window window;
		XScreenSaverInfo xScreenSaverInfo;

		var idleMillis = 0L;
		try
		{
			display = X11.INSTANCE.XOpenDisplay(null);
			window = X11.INSTANCE.XDefaultRootWindow(display);
			xScreenSaverInfo = new XScreenSaverInfo();
			Xss.INSTANCE.XScreenSaverQueryInfo(display, window, xScreenSaverInfo);
			idleMillis = xScreenSaverInfo.idle.longValue();
		}
		finally
		{
			if (display != null)
			{
				X11.INSTANCE.XCloseDisplay(display);
			}
		}
		return (int) (idleMillis / 1000);
	}
}
