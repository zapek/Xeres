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

import java.util.EnumSet;

public class AuthenticationRequirements
{
	public enum Flags
	{
		ROOT_AUTHOR,
		CHILD_AUTHOR,
		ROOT_PUBLISH,
		CHILD_PUBLISH
	}

	private final EnumSet<Flags> publicRequirements;
	private final EnumSet<Flags> restrictedRequirements;
	private final EnumSet<Flags> privateRequirements;
	private boolean optionalAuthor;

	private AuthenticationRequirements(Builder builder)
	{
		publicRequirements = builder.publicRequirements;
		restrictedRequirements = builder.restrictedRequirements;
		privateRequirements = builder.privateRequirements;
		optionalAuthor = builder.optionalRequirements;
	}

	private AuthenticationRequirements(EnumSet<Flags> publicRequirements, EnumSet<Flags> restrictedRequirements, EnumSet<Flags> privateRequirements, boolean optionalAuthor)
	{
		this.publicRequirements = publicRequirements;
		this.restrictedRequirements = restrictedRequirements;
		this.privateRequirements = privateRequirements;
		this.optionalAuthor = optionalAuthor;
	}

	public EnumSet<Flags> getPublicRequirements()
	{
		return publicRequirements;
	}

	public EnumSet<Flags> getRestrictedRequirements()
	{
		return restrictedRequirements;
	}

	public EnumSet<Flags> getPrivateRequirements()
	{
		return privateRequirements;
	}

	public boolean isOptionalAuthor()
	{
		return optionalAuthor;
	}

	public static final class Builder
	{
		private EnumSet<Flags> publicRequirements;
		private EnumSet<Flags> restrictedRequirements;
		private EnumSet<Flags> privateRequirements;
		private boolean optionalRequirements;

		public Builder()
		{
		}

		public Builder withPublic(EnumSet<Flags> val)
		{
			publicRequirements = val;
			return this;
		}

		public Builder withRestricted(EnumSet<Flags> val)
		{
			restrictedRequirements = val;
			return this;
		}

		public Builder withPrivate(EnumSet<Flags> val)
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
