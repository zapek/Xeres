/*
 * Copyright (c) 2019-2021 by David Gerber - https://zapek.com
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

package io.xeres.ui.custom;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.MultipleSelectionModel;

/**
 * Allows to disable the selection; for example, in listviews.
 */
public class NullSelectionModel<T> extends MultipleSelectionModel<T>
{
	@Override
	public ObservableList<Integer> getSelectedIndices()
	{
		return FXCollections.emptyObservableList();
	}

	@Override
	public ObservableList<T> getSelectedItems()
	{
		return FXCollections.emptyObservableList();
	}

	@Override
	public void selectIndices(int index, int... indices)
	{
		// Disabled
	}

	@Override
	public void selectAll()
	{
		// Disabled
	}

	@Override
	public void clearAndSelect(int index)
	{
		// Disabled
	}

	@Override
	public void select(int index)
	{
		// Disabled
	}

	@Override
	public void select(T obj)
	{
		// Disabled
	}

	@Override
	public void clearSelection(int index)
	{
		// Disabled
	}

	@Override
	public void clearSelection()
	{
		// Disabled
	}

	@Override
	public boolean isSelected(int index)
	{
		return false;
	}

	@Override
	public boolean isEmpty()
	{
		return false;
	}

	@Override
	public void selectPrevious()
	{
		// Disabled
	}

	@Override
	public void selectNext()
	{
		// Disabled
	}

	@Override
	public void selectFirst()
	{
		// Disabled
	}

	@Override
	public void selectLast()
	{
		// Disabled
	}
}
