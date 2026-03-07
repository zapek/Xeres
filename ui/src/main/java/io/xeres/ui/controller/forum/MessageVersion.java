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

package io.xeres.ui.controller.forum;

import io.xeres.common.i18n.I18nUtils;
import io.xeres.ui.support.util.DateUtils;

import java.time.Instant;

record MessageVersion(Instant instant, Long id)
{
	@Override
	public String toString()
	{
		return instant != null ? DateUtils.DATE_TIME_PRECISE_FORMAT.format(instant) : I18nUtils.getBundle().getString("latest");
	}
}
