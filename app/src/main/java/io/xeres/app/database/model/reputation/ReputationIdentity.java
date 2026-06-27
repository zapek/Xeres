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

import io.xeres.app.xrs.service.identity.item.IdentityGroupItem;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.LocationIdentifier;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Entity
public class ReputationIdentity
{
	public static final ReputationIdentity DEFAULT_REPUTATION = new ReputationIdentity();

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "identity_id")
	private IdentityGroupItem identity;

	@Embedded
	@AttributeOverride(name = "identifier", column = @Column(name = "gxs_id"))
	@NotNull
	private GxsId gxsId;

	@NotNull
	private Opinion opinion = Opinion.NEUTRAL;

	@NotNull
	private Instant opinionUpdated;

	private Long pgpIdentifier;

	@NotNull
	private Instant lastUsed;

	@ElementCollection
	@Column(name = "opinion")
	private final Map<LocationIdentifier, Opinion> opinions = new HashMap<>();

	private Reputation reputation = Reputation.NEUTRAL;

	@SuppressWarnings("unused")
	public ReputationIdentity()
	{
	}

	/**
	 * Constructor for own opinion about an identity.
	 *
	 * @param gxsId    the gxsId of the identity
	 * @param identity the linked identity; can be null
	 * @param opinion  our own opinion about the identity
	 */
	public ReputationIdentity(GxsId gxsId, IdentityGroupItem identity, Opinion opinion)
	{
		this(gxsId, identity);
		this.opinion = opinion;
	}

	/**
	 * Constructor for remote opinion about an identity.
	 *
	 * @param gxsId              the gxsId of the identity
	 * @param identity           the linked identity; can be null
	 * @param locationIdentifier the location identifier of the peer sending the opinion
	 * @param opinion            the opinion about the identity
	 */
	public ReputationIdentity(GxsId gxsId, IdentityGroupItem identity, LocationIdentifier locationIdentifier, Opinion opinion)
	{
		this(gxsId, identity);
		addOpinion(locationIdentifier, opinion);
	}

	private ReputationIdentity(GxsId gxsId, IdentityGroupItem identity)
	{
		var now = Instant.now();
		this.gxsId = gxsId;
		this.identity = identity;
		if (identity != null)
		{
			if (identity.hasProfile())
			{
				pgpIdentifier = identity.getProfile().getPgpIdentifier();
			}
		}
		opinionUpdated = now;
		lastUsed = now;
	}

	public Opinion getOpinion()
	{
		return opinion;
	}

	public void setOpinion(Opinion opinion)
	{
		this.opinion = opinion;
	}

	public int getOpinionInt()
	{
		return opinion.ordinal();
	}

	public GxsId getGxsId()
	{
		return gxsId;
	}

	public Instant getOpinionUpdated()
	{
		return opinionUpdated;
	}

	public Long getPgpIdentifier()
	{
		return pgpIdentifier;
	}

	public boolean addOpinion(LocationIdentifier locationIdentifier, Opinion opinion)
	{
		Objects.requireNonNull(locationIdentifier);
		if (opinion == Opinion.NEUTRAL)
		{
			throw new IllegalArgumentException("Cannot add NEUTRAL opinion, use removeOpinion() instead");
		}
		var previous = opinions.put(locationIdentifier, opinion);
		return previous != opinion;
	}

	public boolean removeOpinion(LocationIdentifier locationIdentifier)
	{
		return opinions.remove(locationIdentifier) != null;
	}

	public boolean hasPeerOpinions()
	{
		return !opinions.isEmpty();
	}

	public Reputation getReputation()
	{
		return reputation;
	}

	public void setReputation(Reputation reputation)
	{
		this.reputation = reputation;
	}

	public void updateLastUsed()
	{
		lastUsed = Instant.now();
	}

	public Instant getLastUsed()
	{
		return lastUsed;
	}

	public int getNegativeVotes()
	{
		return (int) opinions.entrySet().stream()
				.filter(entry -> entry.getValue() == Opinion.NEGATIVE)
				.count();
	}

	public int getPositiveVotes()
	{
		return (int) opinions.entrySet().stream()
				.filter(entry -> entry.getValue() == Opinion.POSITIVE)
				.count();
	}
}
