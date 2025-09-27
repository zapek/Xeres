/*
 * Copyright (c) 2025 by David Gerber - https://zapek.com
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

package io.xeres.app.service.script;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("unused") // All methods here can be used by JS
public class Console
{
	private static final Logger log = LoggerFactory.getLogger(Console.class);

	private static final String JS_PREFIX = "[JS]";

	public void log(String message)
	{
		info(message);
	}

	public void info(String message)
	{
		log.info(JS_PREFIX + " {}", message);
	}

	public void debug(String message)
	{
		log.debug(JS_PREFIX + " {}", message);
	}

	public void error(String message)
	{
		log.error(JS_PREFIX + " {}", message);
	}

	public void warn(String message)
	{
		log.warn(JS_PREFIX + " {}", message);
	}
}
