/*
 * Copyright (c) 2023-2025 by David Gerber - https://zapek.com
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

public final class AuthenticationRequirements
{
	public enum Flags
	{
		/**
		 * New threads need to be signed by the author of the message. Typical use: forums, since posts are signed.
		 */
		ROOT_AUTHOR,
		/**
		 * All messages need to be signed by the author of the message. Typical use: forums since response to posts are signed, and signed comments in channels.
		 */
		CHILD_AUTHOR,
		/**
		 * New threads need to be signed by the publish signature of the group. Typical use: posts in channels.
		 */
		ROOT_PUBLISH,
		/**
		 * All messages need to be signed by the publish signature of the group. Typical use: channels were comments are restricted to the publisher.
		 */
		CHILD_PUBLISH
	}

	private final Set<Flags> publicRequirements;
	private final Set<Flags> restrictedRequirements;
	private final Set<Flags> privateRequirements;
	private final boolean optionalAuthor;

	private AuthenticationRequirements(Builder builder)
	{
		publicRequirements = builder.publicRequirements;
		restrictedRequirements = builder.restrictedRequirements;
		privateRequirements = builder.privateRequirements;
		optionalAuthor = builder.optionalAuthor;
	}

	public Set<Flags> getPublicRequirements()
	{
		return publicRequirements;
	}

	public Set<Flags> getRestrictedRequirements()
	{
		return restrictedRequirements;
	}

	public Set<Flags> getPrivateRequirements()
	{
		return privateRequirements;
	}

	public boolean isOptionalAuthor()
	{
		return optionalAuthor;
	}

	public static final class Builder
	{
		private Set<Flags> publicRequirements;
		private Set<Flags> restrictedRequirements;
		private Set<Flags> privateRequirements;
		private boolean optionalAuthor;

		public Builder()
		{
			// Default constructor
		}

		public Builder withPublic(Set<Flags> val)
		{
			publicRequirements = val;
			return this;
		}

		public Builder withRestricted(Set<Flags> val)
		{
			restrictedRequirements = val;
			return this;
		}

		public Builder withPrivate(Set<Flags> val)
		{
			privateRequirements = val;
			return this;
		}

		public Builder withOptionalAuthor(boolean val)
		{
			optionalAuthor = val;
			return this;
		}

		public AuthenticationRequirements build()
		{
			return new AuthenticationRequirements(this);
		}
	}
}
