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
 * Pipeline process.
 * <p>It works in the following way.
 * <p>For incoming packets
 * <pre>incoming bytes -> Packet -> Item -> deserialization -> service data</pre>
 * <p>For outgoing packets
 * <pre>service data -> serialization -> Item -> Packet -> outgoing bytes</pre>
 * <p>Right now, the packet encoder sends simple packets. It'll be upgraded to send multi packets later.
 * Both multi packets and simple packets are accepted as input.
 */
package io.xeres.app.net.peer.pipeline;
