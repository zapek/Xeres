/*
 * Copyright (c) 2026 by David Gerber - https://zapek.com
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

/// Retroshare frequenty uses a (deprecated) TLV ([Type-Length-Value](https://en.wikipedia.org/wiki/Type%E2%80%93length%E2%80%93value))
/// system but frequently abuses it in various ways.
///
/// The result is a fairly complicated system which is also completely unnecessary since all RS Items
/// know their own **fixed** structure nullifying the one advantage of TLVs, that is, their ability to be
/// parsed in any order.
package io.xeres.app.xrs.serialization;