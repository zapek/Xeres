/*
 * Copyright (c) 2019-2022 by David Gerber - https://zapek.com
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

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;

import java.util.List;

/**
 * A TextField that is used for read-only fields (like displaying some informative, yet important value).
 * <p>
 * Features:
 * - explanatory look
 * - automatic selection when clicking for easy cut & pasting
 * - context menu to disable the selection
 */
public class ReadOnlyTextField extends TextField
{
	@SuppressWarnings("unused")
	public ReadOnlyTextField()
	{
		super();
		init();
	}

	@SuppressWarnings("unused")
	public ReadOnlyTextField(String text)
	{
		super(text);
		init();
	}

	private void init()
	{
		getStyleClass().add("text-field-readonly");

		setOnMouseClicked(event -> selectAll());

		setContextMenu(createContextMenu());
	}

	private ContextMenu createContextMenu()
	{
		var contextMenu = new ContextMenu();

		contextMenu.getItems().addAll(createDefaultMenuItems());
		var deselect = new MenuItem("Deselect All");
		deselect.setOnAction(event -> deselect());
		contextMenu.getItems().addAll(new SeparatorMenuItem(), deselect);
		return contextMenu;
	}

	private List<MenuItem> createDefaultMenuItems()
	{
		var copy = new MenuItem("Copy");
		copy.setOnAction(event -> copy());

		return List.of(copy);
	}
}
