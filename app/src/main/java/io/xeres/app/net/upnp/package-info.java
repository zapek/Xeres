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
 * UPNP implementation.
 * <p>
 * This is a limited UPNP implementation that finds an active router on the network and sets the
 * proper port forwarding. There is no active listening capabilities (for example, detecting if some new device was turned on)
 * because the use cases for it are limited, and it directly clashes with the OS (for example Windows
 * is already listening on port 1900). Using the OS' UPNP stack would require the use of JNI on Windows, Linux
 * has too many possible setups and OSX is unknown.
 * <p>
 * The goal of this implementation is to be fast and useful in 99% of cases.
 * <p>
 * Theory of operation:
 * <ul>
 *     <li>UPNPService launches a thread</li>
 *     <li>the thread broadcasts a MSEARCH HTTPu query as multicast on port 1900</li>
 *     <li>a router answers with its location URL</li>
 *     <li>the thread connects to the control point and retrieves the control point URL which is described in an XML file</li>
 *     <li>further commands (add mapping, removing mapping, get external ip address) are sent to that control point URL using SOAP</li>
 * </ul>
 */
package io.xeres.app.net.upnp;
