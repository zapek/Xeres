/*
 * Copyright (c) 2024 by David Gerber - https://zapek.com
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

package io.xeres.app.service.backup;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.List;

@XmlRootElement
class Root
{
	private List<PgpId> pgpIDs;
	private List<Group> groups;

	public Root()
	{
		// Default constructor
	}

	@XmlElementWrapper
	@XmlElement(name = "pgpID")
	public List<PgpId> getPgpIDs()
	{
		return pgpIDs;
	}

	public void setPgpIDs(List<PgpId> pgpIDs)
	{
		this.pgpIDs = pgpIDs;
	}

	@XmlElementWrapper
	@XmlElement(name = "group")
	public List<Group> getGroups()
	{
		return groups;
	}

	public void setGroups(List<Group> groups)
	{
		this.groups = groups;
	}
}
