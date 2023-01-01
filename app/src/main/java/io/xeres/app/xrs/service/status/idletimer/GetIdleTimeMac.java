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

package io.xeres.app.xrs.service.status.idletimer;

import com.sun.jna.Library;
import com.sun.jna.Native;
import io.xeres.app.xrs.service.status.GetIdleTime;

public class GetIdleTimeMac implements GetIdleTime
{
	private interface ApplicationServices extends Library
	{
		ApplicationServices INSTANCE = Native.load("ApplicationServices", ApplicationServices.class);

		int kCGAnyInputEventType = ~0;
		int kCGEventSourceStateCombinedSessionState = 0;

		double CGEventSourceSecondsSinceLastEventType(int sourceStateId, int eventType);
	}

	@Override
	public int getIdleTime()
	{
		var idleTimeSeconds = ApplicationServices.INSTANCE.CGEventSourceSecondsSinceLastEventType(
				ApplicationServices.kCGEventSourceStateCombinedSessionState,
				ApplicationServices.kCGAnyInputEventType);
		return (int) idleTimeSeconds;
	}
}
