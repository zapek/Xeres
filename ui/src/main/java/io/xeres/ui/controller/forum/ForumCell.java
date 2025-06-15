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

package io.xeres.ui.controller.forum;

import io.xeres.common.i18n.I18nUtils;
import io.xeres.ui.model.forum.ForumGroup;
import io.xeres.ui.support.util.TooltipUtils;
import javafx.scene.control.TreeTableRow;

import java.text.MessageFormat;
import java.util.ResourceBundle;

public class ForumCell extends TreeTableRow<ForumGroup>
{
	private static final ResourceBundle bundle = I18nUtils.getBundle();

	public ForumCell()
	{
		super();
		TooltipUtils.install(this,
				() -> {
					if (getItem().getId() == 0)
					{
						return null;
					}
					return MessageFormat.format(bundle.getString("forum.tree.info"),
							getItem().getDescription(),
							getItem().getGxsId()
					);
				},
				null);
	}

	@Override
	protected void updateItem(ForumGroup item, boolean empty)
	{
		super.updateItem(item, empty);
		if (empty)
		{
			clearStyle();
		}
		else
		{
			if (item.hasNewMessages())
			{
				setStyle("-fx-font-weight: bold;");
			}
			else
			{
				clearStyle();
			}
		}
	}

	private void clearStyle()
	{
		setStyle("");
	}
}
