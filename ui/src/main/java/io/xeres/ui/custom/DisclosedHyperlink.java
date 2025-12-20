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

package io.xeres.ui.custom;

import io.xeres.common.i18n.I18nUtils;
import io.xeres.ui.support.util.TooltipUtils;
import io.xeres.ui.support.util.UiUtils;
import javafx.beans.NamedArg;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * Special Hyperlink-like class that offers the following benefits:
 * <ul>
 * <li>detects malicious links and warns about them (for example, a link that has a description of <a href="https://foo.com">https://foo.bar</a> but really goes to <a href="https://bar.com">https://bar.com</a>
 * <li>can be reflowed when put on a TextFlow
 * </ul>
 * On the other hand, it doesn't support the "visited" feature of normal hyperlinks.
 */
public class DisclosedHyperlink extends Text
{
	private static final Logger log = LoggerFactory.getLogger(DisclosedHyperlink.class);

	private String uri;
	private boolean malicious;

	private static final ResourceBundle bundle = I18nUtils.getBundle();

	public DisclosedHyperlink(@NamedArg(value = "text") String text, @NamedArg(value = "url") String uri)
	{
		super(text);
		setUri(uri);
		setUnderline(true);
		setStyle("-fx-fill: -color-accent-fg");
		setOnMouseEntered(_ -> setCursor(Cursor.HAND));
		setOnMouseExited(_ -> setCursor(Cursor.DEFAULT));
		UiUtils.setOnPrimaryMouseClicked(this, _ -> {
			var action = onAction.get();
			if (action != null)
			{
				onAction.get().handle(new ActionEvent());
			}
			else
			{
				log.warn("No action defined for hyperlink");
			}
		});
	}

	public final ObjectProperty<EventHandler<ActionEvent>> onActionProperty()
	{
		return onAction;
	}

	public final void setOnAction(EventHandler<ActionEvent> value)
	{
		onActionProperty().set(value);
	}

	public final EventHandler<ActionEvent> getOnAction()
	{
		return onActionProperty().get();
	}

	private final ObjectProperty<EventHandler<ActionEvent>> onAction = new ObjectPropertyBase<>()
	{
		@Override
		protected void invalidated()
		{
			setEventHandler(ActionEvent.ACTION, get());
		}

		@Override
		public Object getBean()
		{
			return DisclosedHyperlink.this;
		}

		@Override
		public String getName()
		{
			return "onAction";
		}
	};

	public String getUri()
	{
		return uri;
	}

	public void setUri(String uri)
	{
		this.uri = uri;
		if (getText().contains("://") && !getText().equals(uri))
		{
			setStyle("-fx-fill: -color-danger-fg;");
			TooltipUtils.install(this, MessageFormat.format(bundle.getString("uri.malicious-link"), uri));
			malicious = true;
		}
		else
		{
			TooltipUtils.install(this, uri);
		}
	}

	public boolean isMalicious()
	{
		return malicious;
	}
}
