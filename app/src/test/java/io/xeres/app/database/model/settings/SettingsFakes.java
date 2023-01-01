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

package io.xeres.app.database.model.settings;

import java.util.concurrent.ThreadLocalRandom;

public final class SettingsFakes
{
	private SettingsFakes()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static Settings createSettings()
	{
		var settings = new Settings();
		settings.setPgpPrivateKeyData(getRandomArray(2000));
		settings.setLocationPrivateKeyData(getRandomArray(2000));
		settings.setLocationPublicKeyData(getRandomArray(500));
		settings.setLocationCertificate(getRandomArray(200));
		return settings;
	}

	private static byte[] getRandomArray(int size)
	{
		var a = new byte[size];
		ThreadLocalRandom.current().nextBytes(a);
		return a;
	}
}
