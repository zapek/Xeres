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

package io.xeres.ui.controller.file;

import io.xeres.common.i18n.I18nEnum;
import io.xeres.common.i18n.I18nUtils;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

import java.util.ResourceBundle;

// Public modifier needed by JavaFX
public class FileProgressDisplay
{
	public enum State implements I18nEnum
	{
		SEARCHING,
		TRANSFERRING,
		REMOVING,
		DONE;

		private final ResourceBundle bundle = I18nUtils.getBundle();

		@Override
		public String toString()
		{
			return bundle.getString(getMessageKey(this));
		}
	}

	private final long id;
	private final SimpleStringProperty name;
	private final SimpleObjectProperty<State> state;
	private final SimpleLongProperty speed;
	private final SimpleDoubleProperty progress;
	private final SimpleLongProperty totalSize;
	private final SimpleStringProperty hash;

	public FileProgressDisplay(long id, String name, State state, long speed, double progress, long totalSize, String hash)
	{
		this.id = id;
		this.name = new SimpleStringProperty(name);
		this.state = new SimpleObjectProperty<>(state);
		this.speed = new SimpleLongProperty(speed);
		this.progress = new SimpleDoubleProperty(progress);
		this.totalSize = new SimpleLongProperty(totalSize);
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

	public long getSpeed()
	{
		return speed.get();
	}

	@SuppressWarnings("unused")
	public SimpleLongProperty speedProperty()
	{
		return speed;
	}

	public void setSpeed(long speed)
	{
		this.speed.set(speed);
	}

	public double getProgress()
	{
		return progress.get();
	}

	@SuppressWarnings("unused")
	public SimpleDoubleProperty progressProperty()
	{
		return progress;
	}

	public void setProgress(double progress)
	{
		this.progress.set(progress);
	}

	@SuppressWarnings("unused")
	public long getTotalSize()
	{
		return totalSize.get();
	}

	@SuppressWarnings("unused")
	public SimpleLongProperty totalSizeProperty()
	{
		return totalSize;
	}

	@SuppressWarnings("unused")
	public void setTotalSize(long totalSize)
	{
		this.totalSize.set(totalSize);
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

	public long getId()
	{
		return id;
	}
}
