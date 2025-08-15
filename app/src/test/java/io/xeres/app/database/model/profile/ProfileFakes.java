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

package io.xeres.app.database.model.profile;

import io.xeres.common.id.ProfileFingerprint;
import io.xeres.testutils.StringFakes;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

import static io.xeres.common.dto.profile.ProfileConstants.OWN_PROFILE_ID;

public final class ProfileFakes
{
	private ProfileFakes()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	private static long id = OWN_PROFILE_ID + 1;

	private static long getUniqueId()
	{
		return id++;
	}

	public static Profile createProfile()
	{
		return createProfile(StringFakes.createNickname(), ThreadLocalRandom.current().nextLong());
	}

	public static Profile createFreshProfile(String name, long pgpIdentifier)
	{
		return new Profile(0L, name, pgpIdentifier, Instant.now(), new ProfileFingerprint(getRandomArray(20)), getRandomArray(200));
	}

	public static Profile createProfile(String name, long pgpIdentifier)
	{
		return createProfile(name, pgpIdentifier, new ProfileFingerprint(getRandomArray(20)), getRandomArray(200));
	}

	public static Profile createProfile(String name, long pgpIdentifier, byte[] pgpFingerprint, byte[] data)
	{
		return new Profile(getUniqueId(), name, pgpIdentifier, Instant.now(), new ProfileFingerprint(pgpFingerprint), data);
	}

	public static Profile createProfile(String name, long pgpIdentifier, ProfileFingerprint profileFingerprint, byte[] data)
	{
		return new Profile(getUniqueId(), name, pgpIdentifier, Instant.now(), profileFingerprint, data);
	}

	public static Profile createOwnProfile()
	{
		return new Profile(1L, StringFakes.createNickname(), ThreadLocalRandom.current().nextLong(), Instant.now(), new ProfileFingerprint(getRandomArray(20)), getRandomArray(200));
	}

	private static byte[] getRandomArray(int size)
	{
		var a = new byte[size];
		ThreadLocalRandom.current().nextBytes(a);
		return a;
	}
}
