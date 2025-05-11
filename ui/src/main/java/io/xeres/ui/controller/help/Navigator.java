/*
 * Copyright (c) 2025 by David Gerber - https://zapek.com
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

package io.xeres.ui.controller.help;

import io.xeres.ui.support.uri.ExternalUri;
import io.xeres.ui.support.uri.Uri;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * This classe handles a navigation using a forward and backwards paradigm. A bit like a web
 * browser but without having to suffer frames navigation (which I believe is broken anyway).
 */
class Navigator
{
	private final List<Uri> history = new ArrayList<>();
	private int historyIndex = -1;

	private final Consumer<Uri> action;

	final BooleanProperty backProperty = new SimpleBooleanProperty(false);
	final BooleanProperty forwardProperty = new SimpleBooleanProperty(false);

	public Navigator(Consumer<Uri> action)
	{
		this.action = action;
	}

	public void navigateBackwards()
	{
		if (historyIndex == 0)
		{
			return;
		}
		action.accept(history.get(--historyIndex));
		updateProperties();
	}

	public void navigateForwards()
	{
		if (historyIndex == history.size() - 1)
		{
			return;
		}
		action.accept(history.get(++historyIndex));
		updateProperties();
	}

	public void navigate(Uri uri)
	{
		Objects.requireNonNull(uri, "uri must not be null");
		if (uri.equals(getCurrentUri()))
		{
			return;
		}
		if (isNavigable(uri)) // We don't want to reopen web URLs when coming back, etc...
		{
			addToHistoryAndTrim(uri);
		}
		action.accept(uri);
		updateProperties();
	}

	public Uri getCurrentUri()
	{
		if (history.isEmpty())
		{
			return null;
		}
		return history.get(historyIndex);
	}

	public boolean isNavigable(Uri uri)
	{
		if (uri instanceof ExternalUri externalUri)
		{
			if (externalUri.toUriString().endsWith(".md"))
			{
				return true;
			}
		}
		return false;
	}

	private void addToHistoryAndTrim(Uri uri)
	{
		while (history.size() - 1 > historyIndex)
		{
			history.removeLast();
		}
		history.addLast(uri);
		historyIndex = history.size() - 1;
	}

	private void updateProperties()
	{
		backProperty.set(historyIndex > 0);
		forwardProperty.set(historyIndex < history.size() - 1);
	}
}
