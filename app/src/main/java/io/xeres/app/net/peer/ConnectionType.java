/*
 * Copyright (c) 2019-2026 by David Gerber - https://zapek.com
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

package io.xeres.app.net.peer;

import io.xeres.common.i18n.I18nEnum;
import io.xeres.common.i18n.I18nUtils;

import java.util.ResourceBundle;

public enum ConnectionType implements I18nEnum
{
	TCP_INCOMING("incoming"),
	TCP_OUTGOING("outgoing"),
	TOR_OUTGOING("Tor"), // Always outgoing
	I2P_OUTGOING("I2P"); // Always outgoing

	private final String loggingDescription;
	private final ResourceBundle bundle = I18nUtils.getBundle();

	ConnectionType(String loggingDescription)
	{
		this.loggingDescription = loggingDescription;
	}

	/**
	 * Gets the description in English, needed for logging.
	 *
	 * @return the description
	 */
	public String getLoggingDescription()
	{
		return loggingDescription;
	}

	@Override
	public String toString()
	{
		return bundle.getString(getMessageKey(this));
	}
}
