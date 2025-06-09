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

package io.xeres.common.mui;

import io.xeres.common.AppName;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
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
	private static JTextArea textArea;
	private static Shell shell;
	private static String currentLine = "";

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
			appendToTextArea("""
					New Shell process 1
					Type 'help' for more information.
					""");
			textArea.setCaretPosition(textArea.getDocument().getLength());
			shellFrame.setVisible(true);
		}
		shellFrame.toFront();
		textArea.requestFocus();
	}

	private static void createShellFrame(Shell shell)
	{
		textArea = new JTextArea();
		textArea.setEditable(true);
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);
		textArea.setMargin(new Insets(8, 8, 8, 8));

		var scrollPane = new JScrollPane(textArea);
		scrollPane.setPreferredSize(new Dimension(640, 320));
		scrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

		textArea.addMouseListener(new MouseListener()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				textArea.setCaretPosition(textArea.getDocument().getLength());
			}

			@Override
			public void mousePressed(MouseEvent e)
			{

			}

			@Override
			public void mouseReleased(MouseEvent e)
			{

			}

			@Override
			public void mouseEntered(MouseEvent e)
			{

			}

			@Override
			public void mouseExited(MouseEvent e)
			{

			}
		});

		textArea.addKeyListener(new KeyListener()
		{
			@Override
			public void keyTyped(KeyEvent e)
			{
				if (e.getKeyChar() == '\n')
				{
					e.consume();
					return;
				}
				else if (e.getKeyChar() == '\b')
				{
					if (!currentLine.isEmpty())
					{
						currentLine = currentLine.substring(0, currentLine.length() - 1);
					}
					return;
				}
				currentLine += e.getKeyChar();
			}

			@Override
			public void keyPressed(KeyEvent e)
			{
				if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE)
				{
					if (currentLine.isEmpty())
					{
						e.consume();
						return;
					}
				}
				else if (e.getKeyCode() == KeyEvent.VK_ENTER)
				{
					e.consume();
					if (shell != null)
					{
						var result = shell.sendCommand(currentLine);
						switch (result.getAction())
						{
							case UNKNOWN_COMMAND -> appendToTextArea(currentLine + ": Unknown command");
							case CLS -> textArea.setText("");
							case EXIT -> closeShell();
							case NO_OP -> appendToTextArea("");
							case SUCCESS -> appendToTextArea(result.getOutput());
						}
					}
					else
					{
						appendToTextArea("No shell interface available");
					}
					currentLine = "";
				}
				if (textArea != null) // We might have typed 'exit'
				{
					textArea.setCaretPosition(textArea.getDocument().getLength());
				}
			}

			@Override
			public void keyReleased(KeyEvent e)
			{
				textArea.setCaretPosition(textArea.getDocument().getLength());
			}
		});

		shellFrame = new JFrame(AppName.NAME + " Shell");
		shellFrame.setIconImage(new ImageIcon(Objects.requireNonNull(MinimalUserInterface.class.getResource("/image/icon.png"))).getImage());
		shellFrame.getContentPane().setLayout(new BoxLayout(shellFrame.getContentPane(), BoxLayout.Y_AXIS));
		shellFrame.add(scrollPane);
		shellFrame.pack();
		shellFrame.setLocationRelativeTo(null);
		shellFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
	}

	private static void appendToTextArea(String text)
	{
		textArea.append("\n" + text + "\n1.SYS:> ");
		textArea.setCaretPosition(textArea.getDocument().getLength());
	}

	public static void closeShell()
	{
		if (shellFrame != null)
		{
			shellFrame.setVisible(false);
			shellFrame.dispose();
			textArea = null;
			shellFrame = null;
		}
	}
}
