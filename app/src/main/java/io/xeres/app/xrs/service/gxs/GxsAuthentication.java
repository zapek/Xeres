/*
 * Copyright (c) 2023-2026 by David Gerber - https://zapek.com
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

package io.xeres.app.xrs.service.gxs;

import java.util.Set;

public final class GxsAuthentication
{
	public enum Flags
	{
		/**
		 * New threads need to be signed by the author of the message. Typical use: forums, since posts are signed.
		 */
		ROOT_NEEDS_AUTHOR,
		/**
		 * All child messages/votes/comments need to be signed by the author of the message. Typical use: forums since response to posts are signed, and signed comments in channels.
		 */
		CHILD_NEEDS_AUTHOR,
		/**
		 * New threads need to be signed by the publish key of the group. Typical use: posts in channels. Only the creator of the group can post.
		 */
		ROOT_NEEDS_PUBLISH,
		/**
		 * All messages/votes/comments need to be signed by the publish key of the group.
		 */
		CHILD_NEEDS_PUBLISH
	}

	private final Set<Flags> requirements;
	private final boolean authorSigningGroups;

	private GxsAuthentication(Builder builder)
	{
		requirements = builder.requirements;
		authorSigningGroups = builder.authorSigningGroups;
	}

	public Set<Flags> getRequirements()
	{
		return requirements;
	}

	public boolean isAuthorSigningGroups()
	{
		return authorSigningGroups;
	}

	public static final class Builder
	{
		private Set<Flags> requirements;
		private boolean authorSigningGroups;

		public Builder()
		{
			// Default constructor
		}

		public Builder withRequirements(Set<Flags> val)
		{
			requirements = val;
			return this;
		}

		public Builder withAuthorSigningGroups(boolean val)
		{
			authorSigningGroups = val;
			return this;
		}

		public GxsAuthentication build()
		{
			return new GxsAuthentication(this);
		}
	}
}
