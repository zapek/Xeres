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

package io.xeres.ui.controller.profile;

import io.xeres.ui.model.profile.Profile;
import javafx.scene.control.*;

public class ProfileCell extends TableRow<Profile>
{
	private final TableView<Profile> listView;

	public ProfileCell(TableView<Profile> listView)
	{
		super();
		this.listView = listView;
		setContextMenu(createContextMenu(this));
	}

	@Override
	protected void updateItem(Profile item, boolean empty)
	{
		super.updateItem(item, empty);
		if (empty)
		{
			clearCell();
		}
		else
		{
			setFullProfile(item.getPgpPublicKeyData() != null);
		}
	}

	private void setFullProfile(boolean full)
	{
		if (full)
		{
			clearCell();
		}
		else
		{
			setStyle("-fx-font-style: italic");
			setTooltip(new Tooltip("Partial profile not fully validated yet"));
		}
	}

	private void clearCell()
	{
		setStyle("");
		setTooltip(null);
	}

	private ContextMenu createContextMenu(TableRow<Profile> cell)
	{
		var contextMenu = new ContextMenu();

		var deleteItem = new MenuItem("Delete profile");
		deleteItem.setOnAction(event -> listView.fireEvent(new ProfileContextMenu(ProfileContextMenu.DELETE, cell.getTableView())));

		contextMenu.getItems().addAll(deleteItem);
		return contextMenu;
	}
}
