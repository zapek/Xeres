/*
 * Copyright (c) 2019-2021 by David Gerber - https://zapek.com
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

package io.xeres.common.rest.profile;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RsIdRequest(
		@NotNull(message = "Missing RS id")
		@Size(min = LENGTH_MIN, max = LENGTH_MAX)
		String rsId
)
{
	private static final int LENGTH_MIN = 8;
	private static final int LENGTH_MAX = 16384;
}
