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

/**
 * Packet format for sending and receiving data.
 * <p>There are 2 header formats for Packets:
 * <p>Old (describing a SimplePacket):<br>
 * <pre>
 * +---------+---------+-----------+--------------------------------------+
 * | version | service | subpacket | size, <b>including</b> header of 8 bytes    |
 * +---------+---------+-----------+--------------------------------------+
 * | 1 byte  | 2 bytes |   1 byte  |                4 bytes               |
 * +---------+---------+-----------+--------------------------------------+
 * </pre>
 * <p>New (describing a MultiPacket, version is always 16):<br>
 * <pre>
 * +---------+---------+------------+--------------------------------------+
 * | version |  flags  | packet id  | size, <b>excluding</b> header of 8 bytes    |
 * +---------+---------+------------+--------------------------------------+
 * | 1 byte  | 1 byte  |   4 bytes  |               2 bytes                |
 * +---------+---------+------------+--------------------------------------+
 * </pre>
 * <p>
 * Checking the protocol version (16) is enough to know if it's a new packet format. The simple packet
 * format is just the Item. The multi packet format is basically the slicing header and the Item as data. It
 * allows grouping and slicing to fit better into 512 bytes long data packets.
 */
package io.xeres.app.net.peer.packet;
