/*
 * Copyright (c) 2019-2022 by David Gerber - https://zapek.com
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

import static org.apache.commons.lang3.StringUtils.isEmpty;

public final class TextFieldUtils
{
	private static final Pattern HOST_PATTERN = Pattern.compile("^([a-zA-Z0-9])?[a-zA-Z0-9.-]{0,253}$");

	private TextFieldUtils()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static void setNumeric(TextField textField, int min, int max)
	{
		if (min < 0 || max < 0)
		{
			throw new IllegalArgumentException("Negative numbers are not supported");
		}
		if (max < min)
		{
			throw new IllegalArgumentException("Max cannot be smaller than min");
		}

		var textFormatter = new TextFormatter<String>(change -> {
			var text = change.getControlNewText();

			if (isEmpty(text))
			{
				return change;
			}
			try
			{
				var value = Integer.parseInt(change.getControlNewText());
				if (value >= min && value <= max)
				{
					return change;
				}
			}
			catch (NumberFormatException ignored)
			{
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
}
