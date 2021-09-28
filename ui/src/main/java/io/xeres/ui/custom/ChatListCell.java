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

import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.text.FontSmoothingType;
import javafx.scene.text.Text;

public class ChatListCell<T extends String> extends ListCell<T>
{
	public ChatListCell(ListView<T> listView)
	{
		Text text = new Text();
		text.setFontSmoothingType(FontSmoothingType.LCD);
		text.wrappingWidthProperty().bind(listView.widthProperty().subtract(15));
		text.textProperty().bind(itemProperty());

		setPrefWidth(0);
		setGraphic(text);
	}
}
