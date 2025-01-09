/*
 * Copyright (c) 2024-2025 by David Gerber - https://zapek.com
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

package io.xeres.ui.support.uri;

import io.xeres.common.id.Id;
import io.xeres.common.id.Identifier;

public record MessageUri(Identifier identifier, String subject) implements Uri
{
	static String AUTHORITY = "message";

	static String PARAMETER_ID = "id";
	static String PARAMETER_SUBJECT = "subject";

	@Override
	public String toString()
	{
		return Uri.buildUri(AUTHORITY,
				PARAMETER_ID, Id.toString(identifier),
				PARAMETER_SUBJECT, subject);
	}
}
