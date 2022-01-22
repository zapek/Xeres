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
import javafx.scene.layout.HBox;
import org.fxmisc.flowless.Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

public class ChatListCell implements Cell<ChatLine, HBox>
{
	private static final Logger log = LoggerFactory.getLogger(ChatListCell.class);

	private static final DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
			.withLocale(Locale.ROOT)
			.withZone(ZoneId.systemDefault());

	private final HBox content;
	private final Label time;
	private final Label label;
	private final ImageView imageView;

	public ChatListCell(ChatLine line)
	{
		content = new HBox();
		content.getStyleClass().add("list-cell");
		time = new Label();
		time.getStyleClass().add("time");
		label = new Label();
		label.setWrapText(true);
		imageView = new ImageView();

		updateItem(line);

		content.getChildren().addAll(time, label, imageView);
	}

	@Override
	public HBox getNode()
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
		label.setText(item.getText());
		imageView.setImage(item.getImage());
	}
}
