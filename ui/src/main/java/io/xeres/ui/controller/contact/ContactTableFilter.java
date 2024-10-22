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

package io.xeres.ui.controller.contact;

import io.xeres.common.rest.contact.Contact;
import javafx.collections.transformation.FilteredList;
import org.apache.commons.lang3.StringUtils;

import java.util.Locale;
import java.util.function.Predicate;

class ContactTableFilter implements Predicate<Contact>
{
	private final FilteredList<Contact> filteredList;

	private boolean hideOtherContacts;
	private boolean hideOtherProfiles;
	private String nameFilter;

	public ContactTableFilter(FilteredList<Contact> filteredList)
	{
		this.filteredList = filteredList;
	}

	public void setHideOtherContacts(boolean hideOtherContacts)
	{
		this.hideOtherContacts = hideOtherContacts;
	}

	public void setHideOtherProfiles(boolean hideOtherProfiles)
	{
		this.hideOtherProfiles = hideOtherProfiles;
	}

	public void setNameFilter(String filter)
	{
		nameFilter = filter;
		filteredList.setPredicate(null); // Force a change otherwise the property will think we're the same predicate
		filteredList.setPredicate(this);
	}

	@Override
	public boolean test(Contact contact)
	{
		if (hideOtherContacts)
		{
			// XXX: needs to know if other...
		}
		if (hideOtherProfiles)
		{
			// XXX: needs to know if not accepted
		}
		if (StringUtils.isNotEmpty(nameFilter))
		{
			return contact.name().toLowerCase(Locale.ROOT).contains(nameFilter.toLowerCase(Locale.ROOT));
		}
		return true;
	}
}
