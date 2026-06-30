/*
 * Copyright (c) 2026 by David Gerber - https://zapek.com
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

package io.xeres.app.database.model.reputation;

import io.xeres.common.id.GxsId;
import io.xeres.testutils.IdFakes;

public final class ReputationIdentityFakes
{
	private ReputationIdentityFakes()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static ReputationIdentity createReputationIdentity()
	{
		return createReputationIdentity(IdFakes.createGxsId(), Opinion.NEUTRAL);
	}

	public static ReputationIdentity createReputationIdentity(Opinion opinion)
	{
		return createReputationIdentity(IdFakes.createGxsId(), opinion);
	}

	public static ReputationIdentity createReputationIdentity(GxsId gxsId, Opinion opinion)
	{
		return new ReputationIdentity(gxsId, null, opinion);
	}

	public static ReputationIdentity createFreshReputationIdentity(GxsId gxsId, Opinion opinion)
	{
		var reputation = new ReputationIdentity(gxsId, null, opinion);
		reputation.updateLastUsed();
		return reputation;
	}
}
