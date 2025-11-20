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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.Objects;

/**
 * MUI: the Minimal User Interface.
 * <p>
 * Just an interface to show some error to the user when failing to start in non-headless mode.
 * <p>
 * Without Xeres, MUI wouldn't exist :)
 */
public final class MUI
{
	private static final Logger log = LoggerFactory.getLogger(MUI.class);

	public static final String PROMPT = "1.SYS:> ";
	private static JFrame shellFrame;
	private static JTextArea textArea;
	private static Shell shell;
	private static String currentLine = "";

	private MUI()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static void setShell(Shell shell)
	{
		MUI.shell = shell;
	}

	/**
	 * Shows an informational message.
	 * <p>
	 * Only use this when JavaFX is not available (for example displaying command arguments on Windows).
	 *
	 * @param message the message to display to the user
	 */
	public static void showInformation(String message)
	{
		JOptionPane.showMessageDialog(null, message, AppName.NAME + " Output", JOptionPane.INFORMATION_MESSAGE);
	}

	/**
	 * Shows an error.
	 * <p>
	 * Only use this when JavaFX is not available. Typically, when its initialization goes wrong.
	 * @param e the Exception
	 */
	public static void showError(Exception e)
	{
		Throwable exception = e;

		while (exception.getCause() != null)
		{
			exception = exception.getCause();
		}
		showError(exception.getMessage());
	}

	private static void showError(String message)
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
		Objects.requireNonNull(shell, "a shell is required");

		textArea = new JTextArea()
		{
			@Override
			public void paste()
			{
				super.paste();
				updateLastLine();
			}
		};
		textArea.setEditable(true);
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);
		textArea.setMargin(new Insets(8, 8, 8, 8));
		textArea.setBackground(Color.GRAY);
		textArea.setForeground(Color.BLACK);

		var scrollPane = new JScrollPane(textArea);
		scrollPane.setPreferredSize(new Dimension(640, 320));
		scrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
		scrollPane.getVerticalScrollBar().setUI(new MUIScrollBar());
		scrollPane.getHorizontalScrollBar().setUI(new MUIScrollBar());

		textArea.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				textArea.setCaretPosition(textArea.getDocument().getLength());
			}
		});

		try
		{
			var font = Font.createFont(Font.TRUETYPE_FONT, Objects.requireNonNull(MUI.class.getResourceAsStream("/topaz.ttf")));
			var derivedFont = font.deriveFont(Font.PLAIN, 14f);
			textArea.setFont(derivedFont);
		}
		catch (FontFormatException | IOException e)
		{
			log.error("Failed to set custom font, guru meditation: {}", e.getMessage());
		}

		textArea.addKeyListener(new KeyListener()
		{
			@Override
			public void keyTyped(KeyEvent e)
			{
				if (e.getKeyChar() == '\n')
				{
					e.consume();
				}
				else if (e.getKeyChar() == '\b')
				{
					if (!currentLine.isEmpty())
					{
						currentLine = currentLine.substring(0, currentLine.length() - 1);
					}
				}
				else if (StringUtils.isAsciiPrintable(String.valueOf(e.getKeyChar()))) // Strip spurious ctrl-v char sequence
				{
					currentLine += e.getKeyChar();
				}
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
					var result = shell.sendCommand(currentLine);
					switch (result.getAction())
					{
						case UNKNOWN_COMMAND -> appendToTextArea(currentLine + ": Unknown command");
						case CLS ->
						{
							textArea.setText("");
							appendToTextArea("");
						}
						case EXIT -> closeShell();
						case NO_OP -> appendToTextArea("");
						case SUCCESS -> appendToTextArea(result.getOutput());
						case ERROR -> appendToTextArea("Error: " + result.getOutput());
					}
					currentLine = "";
				}
				else if (e.getKeyCode() == KeyEvent.VK_UP)
				{
					var previous = shell.getPreviousCommand();
					updateLineHistory(previous);
					e.consume();
				}
				else if (e.getKeyCode() == KeyEvent.VK_DOWN)
				{
					var next = shell.getNextCommand();
					updateLineHistory(next);
					e.consume();
				}
				else if ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) == InputEvent.CTRL_DOWN_MASK)
				{
					return;
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
		shellFrame.setIconImage(new ImageIcon(Objects.requireNonNull(MUI.class.getResource("/image/icon.png"))).getImage());
		shellFrame.getContentPane().setLayout(new BoxLayout(shellFrame.getContentPane(), BoxLayout.Y_AXIS));
		shellFrame.add(scrollPane);
		shellFrame.pack();
		shellFrame.setLocationRelativeTo(null);
		shellFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		shellFrame.addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				closeShell();
			}
		});
	}

	/**
	 * Updates the last line. This is slow, only use it for paste operations or so.
	 */
	private static void updateLastLine()
	{
		String fullText = textArea.getText();
		if (fullText.isEmpty())
		{
			currentLine = "";
		}
		else
		{
			// Split by line breaks and get the last non-empty line
			String[] lines = fullText.split("\\r?\\n");
			currentLine = lines[lines.length - 1]
					.replace(PROMPT, "");
		}
	}

	private static void updateLineHistory(String line)
	{
		if (line == null)
		{
			line = "";
		}

		var pos = textArea.getDocument().getLength();
		textArea.replaceRange(null, pos - currentLine.length(), pos);
		textArea.append(line);
		currentLine = line;
	}

	private static void appendToTextArea(String text)
	{
		if (!textArea.getText().isEmpty())
		{
			textArea.append("\n");
		}
		if (StringUtils.isNotEmpty(text))
		{
			textArea.append(text + "\n");
		}
		textArea.append(PROMPT);
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
