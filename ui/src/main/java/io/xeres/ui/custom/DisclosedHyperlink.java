/*
 * Copyright (c) 2023-2025 by David Gerber - https://zapek.com
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
import javafx.scene.control.Hyperlink;

import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * A class that displays a tooltip over hyperlinks so that one knows what he's going to click on.
 */
public class DisclosedHyperlink extends Hyperlink
{
	private String uri;
	private boolean malicious;

	private final ResourceBundle bundle = I18nUtils.getBundle();

	public DisclosedHyperlink()
	{
		super("");
	}

	public DisclosedHyperlink(String text, String uri)
	{
		super(text);
		setUri(uri);
	}

	public String getUri()
	{
		return uri;
	}

	public void setUri(String uri)
	{
		this.uri = uri;
		if (getText().contains("://") && !getText().equals(uri))
		{
			setStyle("-fx-text-fill: red;");
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
