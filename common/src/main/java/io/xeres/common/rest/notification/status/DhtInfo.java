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

package io.xeres.common.rest.notification.status;

public record DhtInfo(DhtStatus dhtStatus, int numPeers, long receivedPackets, long receivedBytes, long sentPackets, long sentBytes, int keyCount, int itemCount)
{
	public static DhtInfo fromStatus(DhtStatus dhtStatus)
	{
		return new DhtInfo(dhtStatus, 0, 0, 0, 0, 0, 0, 0);
	}

	public static DhtInfo fromStats(int numPeers, long receivedPackets, long receivedBytes, long sentPackets, long sentBytes, int keyCount, int itemCount)
	{
		return new DhtInfo(DhtStatus.RUNNING, numPeers, receivedPackets, receivedBytes, sentPackets, sentBytes, keyCount, itemCount);
	}
}
