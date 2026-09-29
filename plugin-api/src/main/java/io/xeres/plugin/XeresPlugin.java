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

package io.xeres.plugin;

import java.util.List;

/// Startup-only extension. Providers are discovered through Java ServiceLoader.
/// Plugins execute trusted code in the host process and must match the API version.
public interface XeresPlugin
{
	int API_VERSION = 2;

	String id();

	int apiVersion();

	/// Spring component/configuration classes to register before context refresh.
	List<Class<?>> components();

	/// Optional GUI components. Never resolved or instantiated in headless mode.
	default List<Class<?>> uiComponents()
	{
		return List.of();
	}
}
