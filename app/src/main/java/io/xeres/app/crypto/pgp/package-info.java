/*
 * Copyright (c) 2019-2020 by David Gerber - https://zapek.com
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
 * Implements all PGP related functions. Used for creating the private and public PGP keys
 * which identify one profile, also known as a user. Locations' certificates are then signed using
 * the <i>private key</i>.<p>
 * The <i>public key</i> is distributed to other profiles so that they can verify the location's certificate
 * signature.
 *
 * @see <a href="https://tools.ietf.org/html/rfc4880">RFC 4880</a>
 */
package io.xeres.app.crypto.pgp;
