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
import javafx.scene.control.TreeItem;
import org.apache.commons.lang3.StringUtils;

import java.util.Locale;
import java.util.function.Predicate;

class ContactFilter implements Predicate<TreeItem<Contact>>
{
	private final FilteredList<TreeItem<Contact>> filteredList;

	private boolean showAllContacts = true;
	private String nameFilter;

	public ContactFilter(FilteredList<TreeItem<Contact>> filteredList)
	{
		this.filteredList = filteredList;
	}

	public void setShowAllContacts(boolean showAllContacts)
	{
		this.showAllContacts = showAllContacts;
		changePredicate();
	}

	public void setNameFilter(String filter)
	{
		nameFilter = filter;
		changePredicate();
	}

	/**
	 * Forces a change of predicate, otherwise the property will think we're the same.
	 */
	private void changePredicate()
	{
		filteredList.setPredicate(null);
		filteredList.setPredicate(this);
	}

	@Override
	public boolean test(TreeItem<Contact> contact)
	{
		if (StringUtils.isNotEmpty(nameFilter))
		{
			return contact.getValue().name().toLowerCase(Locale.ROOT).contains(nameFilter.toLowerCase(Locale.ROOT)) && (showAllContacts || contact.getValue().accepted());
		}
		return showAllContacts || contact.getValue().accepted();
	}
}
