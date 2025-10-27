/*
 * Copyright (c) 2019-2025 by David Gerber - https://zapek.com
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.IllformedLocaleException;
import java.util.Locale;
import java.util.ResourceBundle;

public final class I18nUtils
{
	private static final Logger log = LoggerFactory.getLogger(I18nUtils.class);

	private static final String BUNDLE = "i18n.messages";

	private static ResourceBundle resourceBundle;

	private I18nUtils()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	/**
	 * Gets the ResourceBundle.
	 * <p>
	 * Note: prefer using the ResourceBundle bean for spring components and non-static methods.
	 *
	 * @return the resource bundle
	 */
	public static ResourceBundle getBundle()
	{
		if (resourceBundle == null)
		{
			var envLanguage = System.getenv("XERES_LANGUAGE");
			if (envLanguage != null)
			{
				try
				{
					Locale.setDefault(new Locale.Builder().setLanguage(envLanguage).build());
				}
				catch (IllformedLocaleException e)
				{
					log.error("Locale {} is ill formed: {}", envLanguage, e.getMessage());
				}
			}
			resourceBundle = ResourceBundle.getBundle(BUNDLE);
		}
		return resourceBundle;
	}
}
