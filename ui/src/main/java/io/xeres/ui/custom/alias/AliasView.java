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

package io.xeres.ui.custom.alias;

import io.xeres.ui.support.chat.AliasEntry;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

class AliasView extends VBox
{
	interface OnActionListener
	{
		void complete(String action);

		void cancel();
	}

	@FXML
	private ListView<AliasEntry> aliasList;

	private OnActionListener onActionListener;

	private FilteredList<AliasEntry> filteredList;

	public AliasView()
	{
		var loader = new FXMLLoader(AliasView.class.getResource("/view/custom/alias_view.fxml"));
		loader.setRoot(this);
		loader.setController(this);

		try
		{
			loader.load();
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}

		aliasList.setCellFactory(_ -> new AliasCell());
		addEventFilter(KeyEvent.KEY_PRESSED, event -> {
			if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.TAB)
			{
				action();
			}
			else if (event.getCode() == KeyCode.ESCAPE)
			{
				if (onActionListener != null)
				{
					onActionListener.cancel();
				}
			}
		});
		addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
			if (event.getButton() == MouseButton.PRIMARY)
			{
				action();
			}
		});
	}

	private void action()
	{
		if (onActionListener != null)
		{
			var alias = aliasList.getSelectionModel().getSelectedItem();
			if (alias != null)
			{
				onActionListener.complete(getAliasString(alias));
			}
			else
			{
				if (filteredList.size() == 1)
				{
					alias = filteredList.getFirst();
					onActionListener.complete(getAliasString(alias));
				}
				else
				{
					onActionListener.complete(null);
				}
			}
		}
	}

	private static String getAliasString(AliasEntry alias)
	{
		return "/" + alias.name() + ((alias.required() != null || alias.optional() != null) ? " " : "");
	}

	public void setListener(OnActionListener onActionListener)
	{
		this.onActionListener = onActionListener;
	}

	public void setAliasList(List<AliasEntry> entries)
	{
		filteredList = new FilteredList<>(FXCollections.observableArrayList(entries), _ -> true);
		aliasList.setItems(filteredList);

		if (!entries.isEmpty())
		{
			aliasList.getSelectionModel().selectFirst();
		}
		aliasList.getSelectionModel().selectedItemProperty().addListener((_, oldValue, newValue) -> {
			if (newValue == null && !aliasList.getItems().isEmpty())
			{
				Platform.runLater(() -> {
					// Try to reselect the old value if it still exists
					if (oldValue != null && aliasList.getItems().contains(oldValue))
					{
						aliasList.getSelectionModel().select(oldValue);
					}
					else
					{
						// Otherwise, select the first
						aliasList.getSelectionModel().selectFirst();
					}
				});
			}
		});
	}

	public void setFilter(String text)
	{
		filteredList.setPredicate(aliasEntry -> {
			if (StringUtils.isEmpty(text))
			{
				return true;
			}
			var textLw = text.toLowerCase(Locale.ENGLISH);
			var aliasLw = ("/" + aliasEntry.name()).toLowerCase(Locale.ROOT);

			return aliasLw.contains(textLw) || textLw.contains(aliasLw);
		});
	}
}
