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

import io.xeres.ui.controller.chat.ChatLine;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.fxmisc.flowless.Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

public class ChatListCell implements Cell<ChatLine, TextFlow>
{
	private static final Logger log = LoggerFactory.getLogger(ChatListCell.class);

	private static final DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
			.withLocale(Locale.ROOT)
			.withZone(ZoneId.systemDefault());

	private final TextFlow content;
	private final Label time;
	private final Label action;
	private final Text message;
	private final ImageView imageView;

	public ChatListCell(ChatLine line)
	{
		content = new TextFlow();
		content.getStyleClass().add("list-cell");

		time = new Label();
		time.getStyleClass().add("time");

		action = new Label();
		action.getStyleClass().add("action");

		message = new Text();
		imageView = new ImageView();

		updateItem(line);

		content.getChildren().addAll(time, action, message, imageView);
	}

	@Override
	public TextFlow getNode()
	{
		return content;
	}

	@Override
	public boolean isReusable()
	{
		return true;
	}

	@Override
	public void updateItem(ChatLine item)
	{
		time.setText(formatter.format(item.getInstant()));
		action.setText(item.getAction());
		message.setText(item.getMessage());
		imageView.setImage(item.getImage());
	}
}
