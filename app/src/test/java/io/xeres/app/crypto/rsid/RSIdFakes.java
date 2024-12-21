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

package io.xeres.app.crypto.rsid;

import io.xeres.app.database.model.location.LocationFakes;
import io.xeres.app.database.model.profile.Profile;
import io.xeres.app.database.model.profile.ProfileFakes;
import io.xeres.testutils.StringFakes;

import static io.xeres.common.rsid.Type.CERTIFICATE;
import static io.xeres.common.rsid.Type.SHORT_INVITE;

public final class RSIdFakes
{
	private RSIdFakes()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static RSId createShortInvite()
	{
		var profile = ProfileFakes.createProfile();

		var builder = new RSIdBuilder(SHORT_INVITE);
		return builder.setName(StringFakes.createNickname().getBytes())
				.setLocationIdentifier(LocationFakes.createLocation().getLocationIdentifier())
				.setPgpFingerprint(profile.getProfileFingerprint().getBytes())
				.build();
	}

	public static RSId createRsCertificate()
	{
		var builder = new RSIdBuilder(CERTIFICATE);
		return builder.setName(StringFakes.createNickname().getBytes())
				.setProfile(ProfileFakes.createProfile())
				.setLocationIdentifier(LocationFakes.createLocation().getLocationIdentifier())
				.build();
	}

	public static RSId createRsCertificate(Profile profile)
	{
		var builder = new RSIdBuilder(CERTIFICATE);
		return builder.setName(profile.getName().getBytes())
				.setProfile(profile)
				.setLocationIdentifier(LocationFakes.createLocation().getLocationIdentifier())
				.build();
	}
}
