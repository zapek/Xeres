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

package io.xeres.common.reputation;

/**
 * The reputation of an identity. Is the result from the computation
 * of several parameters.
 */
public enum Reputation
{
	/**
	 * Negative local opinion.
	 */
	LOCALLY_NEGATIVE,

	/**
	 * Neutral local opinion, friends are negative on average.
	 */
	REMOTELY_NEGATIVE,

	/**
	 * No reputation information. The default state.
	 */
	NEUTRAL,

	/**
	 * Neutral local opinion, friends are positive on average.
	 */
	REMOTELY_POSITIVE,

	/**
	 * Positive local opinion.
	 */
	LOCALLY_POSITIVE
}
