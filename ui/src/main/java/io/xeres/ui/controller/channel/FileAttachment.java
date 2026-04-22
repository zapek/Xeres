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

package io.xeres.ui.controller.channel;

import io.xeres.common.i18n.I18nEnum;
import io.xeres.common.i18n.I18nUtils;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

import java.util.ResourceBundle;

// Public modifier needed by JavaFX
public class FileAttachment
{
	public enum State implements I18nEnum
	{
		HASHING,
		DONE;

		private final ResourceBundle bundle = I18nUtils.getBundle();

		@Override
		public String toString()
		{
			return bundle.getString(getMessageKey(this));
		}
	}

	private final SimpleStringProperty name;
	private final SimpleStringProperty path;
	private final SimpleObjectProperty<State> state;
	private final SimpleLongProperty size;
	private final SimpleStringProperty hash;

	FileAttachment(String name, String path, State state, long size, String hash)
	{
		this.name = new SimpleStringProperty(name);
		this.path = new SimpleStringProperty(path);
		this.state = new SimpleObjectProperty<>(state);
		this.size = new SimpleLongProperty(size);
		this.hash = new SimpleStringProperty(hash);
	}

	public String getName()
	{
		return name.get();
	}

	@SuppressWarnings("unused")
	public SimpleStringProperty nameProperty()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name.set(name);
	}

	public String getPath()
	{
		return path.get();
	}

	@SuppressWarnings("unused")
	public SimpleStringProperty pathProperty()
	{
		return path;
	}

	public void setPath(String path)
	{
		this.path.set(path);
	}

	public State getState()
	{
		return state.get();
	}

	@SuppressWarnings("unused")
	public SimpleObjectProperty<State> stateProperty()
	{
		return state;
	}

	public void setState(State state)
	{
		this.state.set(state);
	}

	public long getSize()
	{
		return size.get();
	}

	@SuppressWarnings("unused")
	public SimpleLongProperty sizeProperty()
	{
		return size;
	}

	public void setSize(long size)
	{
		this.size.set(size);
	}

	public String getHash()
	{
		return hash.get();
	}

	@SuppressWarnings("unused")
	public SimpleStringProperty hashProperty()
	{
		return hash;
	}

	public void setHash(String hash)
	{
		this.hash.set(hash);
	}
}
