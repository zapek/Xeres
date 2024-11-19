/*
 * Copyright (c) 2019-2023 by David Gerber - https://zapek.com
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
import java.util.Objects;

/**
 * MUI: the Minimal User Interface.
 * <p>
 * Just an interface to show some error to the user when failing to start in non-headless mode.
 * <p>
 * Without Xeres, MUI wouldn't exist :)
 */
public final class MinimalUserInterface
{
	private static JFrame shellFrame;
	private static JTextField textField;
	private static JTextArea textArea;
	private static Shell shell;

	private MinimalUserInterface()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static void setShell(Shell shell)
	{
		MinimalUserInterface.shell = shell;
	}

	public static void showInformation(String message)
	{
		JOptionPane.showMessageDialog(null, message, AppName.NAME + " Output", JOptionPane.INFORMATION_MESSAGE);
	}

	public static void showError(Exception e)
	{
		Throwable exception = e;

		while (exception.getCause() != null)
		{
			exception = exception.getCause();
		}
		showError(exception.getMessage());

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

	public static void openShell()
	{
		if (shellFrame == null)
		{
			createShellFrame(shell);
		}
		if (!shellFrame.isVisible())
		{
			textArea.setText("""
					New Shell process 1
					Type 'help' for more information.
					""");
			textField.setText("");
			shellFrame.setVisible(true);
		}
		shellFrame.toFront();
		textField.requestFocus();
	}

	private static void createShellFrame(Shell shell)
	{
		textArea = new JTextArea();
		textArea.setEditable(false);
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);
		textArea.setMargin(new Insets(8, 8, 8, 8));

		var scrollPane = new JScrollPane(textArea);
		scrollPane.setPreferredSize(new Dimension(640, 320));
		scrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

		textField = new JTextField();
		textField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
		textField.addActionListener(e -> {
			textField.setText("");
			if (shell != null)
			{
				var result = shell.sendCommand(e.getActionCommand());
				switch (result.getAction())
				{
					case UNKNOWN_COMMAND -> appendToTextArea(textArea, e.getActionCommand() + ": Unknown command");
					case CLS -> textArea.setText("");
					case EXIT -> closeShell();
					case NO_OP -> appendToTextArea(textArea, "");
					case SUCCESS -> appendToTextArea(textArea, result.getOutput());
				}
			}
			else
			{
				appendToTextArea(textArea, "No shell interface available");
			}
		});

		shellFrame = new JFrame(AppName.NAME + " Shell");
		shellFrame.setIconImage(new ImageIcon(Objects.requireNonNull(MinimalUserInterface.class.getResource("/image/icon.png"))).getImage());
		shellFrame.getContentPane().setLayout(new BoxLayout(shellFrame.getContentPane(), BoxLayout.Y_AXIS));
		shellFrame.add(scrollPane);
		shellFrame.add(textField);
		shellFrame.pack();
		shellFrame.setLocationRelativeTo(null);
		shellFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
	}

	private static void appendToTextArea(JTextArea textArea, String text)
	{
		textArea.append(text + "\n");
		textArea.setCaretPosition(textArea.getDocument().getLength());
	}

	public static void closeShell()
	{
		if (shellFrame != null)
		{
			shellFrame.setVisible(false);
			shellFrame.dispose();
		}
	}
}
