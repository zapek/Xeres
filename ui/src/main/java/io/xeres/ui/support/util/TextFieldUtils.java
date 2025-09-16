/*
 * Copyright (c) 2019-2025 by David Gerber - https://zapek.com
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

package io.xeres.ui.support.util;

import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isEmpty;

public final class TextFieldUtils
{
	private static final Pattern HOST_PATTERN = Pattern.compile("^([a-zA-Z0-9])?[a-zA-Z0-9.-]{0,253}$");

	private TextFieldUtils()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static void setNumeric(TextField textField, int minChars, int maxChars)
	{
		if (minChars < 0 || maxChars < 0)
		{
			throw new IllegalArgumentException("Negative char limits are not supported");
		}
		if (maxChars < minChars)
		{
			throw new IllegalArgumentException("maxChars cannot be smaller than minChars");
		}

		var textFormatter = new TextFormatter<String>(change -> {
			var text = change.getControlNewText();

			if (isEmpty(text))
			{
				return change;
			}
			try
			{
				Integer.parseInt(change.getControlNewText());
				if (change.getControlNewText().length() >= minChars && change.getControlNewText().length() <= maxChars)
				{
					return change;
				}
			}
			catch (NumberFormatException _)
			{
				// nothing to do
			}
			return null;
		});
		textField.setTextFormatter(textFormatter);
	}

	public static void setHost(TextField textField)
	{
		var textFormatter = new TextFormatter<String>(change -> HOST_PATTERN.matcher(change.getControlNewText()).matches() ? change : null);
		textField.setTextFormatter(textFormatter);
	}

	public static String getString(TextField textField)
	{
		return isBlank(textField.getText()) ? null : textField.getText();
	}

	public static int getAsNumber(TextField textField)
	{
		try
		{
			return Integer.parseInt(textField.getText());
		}
		catch (NumberFormatException _)
		{
			return 0;
		}
	}
}
