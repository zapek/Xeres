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

import atlantafx.base.theme.Styles;
import io.xeres.ui.support.chat.AliasEntry;
import io.xeres.ui.support.util.TooltipUtils;
import io.xeres.ui.support.util.UiUtils;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.apache.commons.lang3.StringUtils;

public class AliasCell extends ListCell<AliasEntry>
{
	private VBox vbox;
	private Label name;
	private Label required;
	private Label optional;
	private Label description;

	@Override
	protected void updateItem(AliasEntry item, boolean empty)
	{
		super.updateItem(item, empty);
		setGraphic(empty ? null : updateAlias(item));
	}

	private VBox updateAlias(AliasEntry entry)
	{
		if (vbox == null)
		{
			name = new Label();
			required = new Label();
			TooltipUtils.install(required, "Required");
			required.getStyleClass().add(Styles.ACCENT);
			optional = new Label();
			TooltipUtils.install(optional, "Optional");
			optional.getStyleClass().add(Styles.TEXT_SUBTLE);
			description = new Label(entry.description());
			description.getStyleClass().add(Styles.TEXT_SMALL);
			description.getStyleClass().add(Styles.TEXT_MUTED);
			var hbox = new HBox(name, required, optional);
			hbox.setSpacing(4);
			vbox = new VBox(hbox, description);
		}
		name.setText("/" + entry.name());
		required.setText(entry.required());
		UiUtils.setAbsent(required, StringUtils.isEmpty(entry.required()));
		optional.setText(entry.optional());
		UiUtils.setAbsent(optional, StringUtils.isEmpty(entry.optional()));
		description.setText(entry.description());
		return vbox;
	}
}
