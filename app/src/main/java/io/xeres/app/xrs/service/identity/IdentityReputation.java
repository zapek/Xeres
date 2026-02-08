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

package io.xeres.app.xrs.service.identity;

import io.xeres.app.xrs.service.identity.item.IdentityGroupItem;

/**
 * Class to handle identity's reputational score. This mostly depends on the identity's affinity to a profile,
 * our friends' opinion of the identity and our own opinion.
 */
final class IdentityReputation
{
	/**
	 * Identity's profile is known to us. This gives a high score.
	 */
	private static final int PROFILE_KNOWN_SCORE = 50;

	/**
	 * Identity is linked to a profile. This gives a middle score.
	 */
	private static final int PROFILE_UNKNOWN_SCORE = 20;

	/**
	 * Identity is not linked to a profile. This gives the lowest score.
	 */
	private static final int ANONYMOUS_SCORE = 5;

	private IdentityReputation()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	/**
	 * Updates the identity reputation score.
	 *
	 * @param identity      the identity to update
	 * @param profileLinked if the identity is linked to a profile
	 * @param profileKnown  if the identity's profile is known to us
	 */
	public static void updateScore(IdentityGroupItem identity, boolean profileLinked, boolean profileKnown)
	{
		int identityScore;

		if (profileLinked)
		{
			if (profileKnown)
			{
				identityScore = PROFILE_KNOWN_SCORE;
			}
			else
			{
				identityScore = PROFILE_UNKNOWN_SCORE;
			}
		}
		else
		{
			identityScore = ANONYMOUS_SCORE;
		}
		identity.setIdentityScore(identityScore);
		identity.setOverallScore(identityScore + identity.getOwnOpinion() + identity.getPeerOpinion());
	}
}
