/*
 * Copyright (c) 2019-2025 by David Gerber - https://zapek.com
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

import io.xeres.common.i18n.I18nUtils;
import io.xeres.ui.support.util.UiUtils;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;

import java.util.List;
import java.util.ResourceBundle;

/**
 * A TextField that is used for read-only fields (like displaying some informative, yet important value). It features:
 * <p>
 * <ul>
 * <li>explanatory look
 * <li>automatic selection when clicking for easy cut &amp; pasting
 * <li>context menu to disable the selection
 * </ul>
 */
public class ReadOnlyTextField extends TextField
{
	private static final ResourceBundle bundle = I18nUtils.getBundle();

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
		UiUtils.setOnPrimaryMouseClicked(this, event -> selectAll());
		setEditable(false);

		setContextMenu(createContextMenu());
	}

	private ContextMenu createContextMenu()
	{
		var contextMenu = new ContextMenu();

		contextMenu.getItems().addAll(createDefaultMenuItems());
		var deselect = new MenuItem(bundle.getString("deselect-all"));
		deselect.setOnAction(event -> deselect());
		contextMenu.getItems().addAll(new SeparatorMenuItem(), deselect);
		return contextMenu;
	}

	private List<MenuItem> createDefaultMenuItems()
	{
		var copy = new MenuItem(bundle.getString("copy"));
		copy.setOnAction(event -> copy());

		return List.of(copy);
	}
}
