/*
 * Copyright (c) 2023 by David Gerber - https://zapek.com
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

package io.xeres.ui;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;

/**
 * Extend your test from this abstract class if you don't use testfx's ApplicationExtension.class
 * (for example you use Spring Boot's SpringExtension.class.<br>
 * Note that depending on how you run the tests, the platform might already be running.
 */
abstract public class FXTest
{
	@BeforeAll
	static void initJfxRuntime()
	{
		try
		{
			Platform.startup(() -> {
			});
		}
		catch (IllegalStateException e)
		{
			// Platform already running, just ignore
		}
	}
}
