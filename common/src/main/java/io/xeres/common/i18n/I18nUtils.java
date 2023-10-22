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

package io.xeres.common.i18n;

import java.util.ResourceBundle;

public final class I18nUtils
{
	public static final String BUNDLE = "i18n.messages";

	private I18nUtils()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	/**
	 * Gets a translated string. Prefer using the ResourceBundle bean instead as it's faster.
	 *
	 * @param messageKey the message key
	 * @return the translated string
	 */
	public static String getString(String messageKey)
	{
		return getBundle().getString(messageKey);
	}

	/**
	 * Gets the ResourceBundle. Prefer using the ResourceBundle bean instead as it's faster.
	 *
	 * @return the resource bundle
	 */
	public static ResourceBundle getBundle()
	{
		return ResourceBundle.getBundle(BUNDLE);
	}
}
