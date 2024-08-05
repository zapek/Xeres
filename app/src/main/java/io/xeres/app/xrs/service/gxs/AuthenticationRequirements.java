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

package io.xeres.app.xrs.service.gxs;

import java.util.Set;

public final class AuthenticationRequirements
{
	public enum Flags
	{
		ROOT_AUTHOR,
		CHILD_AUTHOR,
		ROOT_PUBLISH,
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
		optionalAuthor = builder.optionalRequirements;
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
		private boolean optionalRequirements;

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

		public Builder withOptional(boolean val)
		{
			optionalRequirements = val;
			return this;
		}

		public AuthenticationRequirements build()
		{
			return new AuthenticationRequirements(this);
		}
	}
}
