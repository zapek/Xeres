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

import io.xeres.ui.support.chat.ChatContent;
import io.xeres.ui.support.chat.ChatLine;
import javafx.scene.control.Label;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.fxmisc.flowless.Cell;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

public class ChatListCell implements Cell<ChatLine, TextFlow>
{
	private static final DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
			.withLocale(Locale.ROOT)
			.withZone(ZoneId.systemDefault());

	private final TextFlow content;
	private final Label time;
	private final Label action;
	private boolean isComplex;

	public ChatListCell(ChatLine line)
	{
		content = new TextFlow();
		content.getStyleClass().add("list-cell");

		time = new Label();
		time.getStyleClass().add("time");

		action = new Label();
		action.getStyleClass().add("action");

		content.getChildren().addAll(time, action);

		updateItem(line);
	}

	@Override
	public TextFlow getNode()
	{
		return content;
	}

	@Override
	public boolean isReusable()
	{
		return !isComplex;
	}

	@Override
	public void reset()
	{
		if (isReusable())
		{
			content.getChildren().remove(2);
		}
	}

	@Override
	public void updateItem(ChatLine line)
	{
		isComplex = line.isRich();

		time.setText(formatter.format(line.getInstant()));
		action.setText(line.getAction());
		action.setTextFill(line.getNicknameColor());

		var nodes = line.getChatContents().stream()
				.map(ChatContent::getNode)
				.toList();

		if (nodes.size() == 1 && nodes.get(0) instanceof Text text) // XXX: check if that works for single URLs..
		{
			text.setFill(line.getContentColor());
		}

		content.getChildren().addAll(nodes);
	}
}
