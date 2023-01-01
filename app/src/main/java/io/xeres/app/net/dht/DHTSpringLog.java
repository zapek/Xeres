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

package io.xeres.app.net.dht;

import lbms.plugins.mldht.kad.DHT.LogLevel;
import lbms.plugins.mldht.kad.DHTLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DHTSpringLog implements DHTLogger
{
	private static final Logger log = LoggerFactory.getLogger(DHTSpringLog.class);

	private static final String EXCEPTION_HEADING = "Exception : ";

	@Override
	public void log(String s, LogLevel logLevel)
	{
		switch (logLevel)
		{
			case Fatal -> log.error(s);
			case Error -> log.warn(s);
			case Info -> log.info(s);
			case Debug -> log.debug(s);
			case Verbose -> log.trace(s);
		}
	}

	@Override
	public void log(Throwable throwable, LogLevel logLevel)
	{
		switch (logLevel)
		{
			case Fatal -> log.error(EXCEPTION_HEADING, throwable);
			case Error -> log.warn(EXCEPTION_HEADING, throwable);
			case Info -> log.info(EXCEPTION_HEADING, throwable);
			case Debug -> log.debug(EXCEPTION_HEADING, throwable);
			case Verbose -> log.trace(EXCEPTION_HEADING, throwable);
		}
	}

	public static LogLevel getLogLevel()
	{
		if (log.isTraceEnabled())
		{
			return LogLevel.Verbose;
		}
		else if (log.isDebugEnabled())
		{
			return LogLevel.Debug;
		}
		else if (log.isInfoEnabled())
		{
			return LogLevel.Info;
		}
		else if (log.isWarnEnabled())
		{
			return LogLevel.Error;
		}
		else if (log.isErrorEnabled())
		{
			return LogLevel.Fatal;
		}
		return LogLevel.Info;
	}
}
