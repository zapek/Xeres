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

package io.xeres.common.mui;

import io.xeres.common.AppName;

import javax.swing.*;
import java.awt.*;

/**
 * MUI: the Minimal User Interface.
 * <p>
 * Just an interface to show some error to the user when failing to start in non-headless mode.
 * <p>
 * Without Xeres, MUI wouldn't exist :)
 */
public final class MinimalUserInterface
{
	private MinimalUserInterface()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static void showInformation(String message)
	{
		JOptionPane.showMessageDialog(null, message, AppName.NAME + " Output", JOptionPane.INFORMATION_MESSAGE);
	}

	public static void showError(String message)
	{
		var scrollPane = new JScrollPane();
		scrollPane.setPreferredSize(new Dimension(640, 240));
		var textArea = new JTextArea(message);
		textArea.setEditable(false);
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);
		textArea.setMargin(new Insets(8, 8, 8, 8));
		scrollPane.getViewport().setView(textArea);

		JOptionPane.showMessageDialog(null, scrollPane, AppName.NAME + " Runtime Problem", JOptionPane.ERROR_MESSAGE);
	}
}
