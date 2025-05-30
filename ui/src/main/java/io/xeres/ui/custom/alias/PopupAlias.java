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

import io.xeres.ui.support.chat.ChatCommand;
import javafx.geometry.Bounds;
import javafx.stage.Popup;
import javafx.stage.PopupWindow;

import java.util.function.Consumer;

public class PopupAlias extends Popup
{
	private final AliasView aliasView;

	public PopupAlias(Bounds bounds, Consumer<String> complete)
	{
		super();

		aliasView = new AliasView();
		setAnchorX(bounds.getMinX());
		setAnchorY(bounds.getMinY());
		setAnchorLocation(PopupWindow.AnchorLocation.CONTENT_BOTTOM_LEFT);

		getContent().add(aliasView);
		setAutoHide(true);

		aliasView.setAliasList(ChatCommand.ALIASES);

		aliasView.setListener(new AliasView.OnActionListener()
		{
			@Override
			public void complete(String action)
			{
				if (complete != null)
				{
					complete.accept(action);
					hide();
				}
			}

			@Override
			public void cancel()
			{
				hide();
			}
		});
	}

	public void setFilter(String text)
	{
		aliasView.setFilter(text);
	}
}
