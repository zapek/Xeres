/*
 * Copyright (c) 2024-2025 by David Gerber - https://zapek.com
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

package io.xeres.ui.support.clipboard;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Utility class to use the clipboard. This implementation uses AWT because the clipboard support of JavaFX is, quite frankly, a
 * royal piece of shit:
 * <ul>
 *     <li>it fails to work with some bitmaps (for example from Telegram, Windows 10 and print screen, Chrome, ...).
 * <li>it fails with data URIs because it tries to find out if the image is a supported format and even though it is, the URL is "wrong" for it.
 * </ul>
 * <p>
 * This one just works. Note that there still might be some warnings printed out because of the DataFlavor system that isn't compatible
 * with everything. It's harmless though.
 */
public final class ClipboardUtils
{
	private static final Logger log = LoggerFactory.getLogger(ClipboardUtils.class);

	private ClipboardUtils()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	/**
	 * Gets whatever is in the clipboard and supported, currently: string and JavaFX images.
	 *
	 * @return a string or an image
	 */
	public static Object getSupportedObjectFromClipboard()
	{
		Object object = getImageFromClipboard();
		if (object == null)
		{
			object = getStringFromClipboard();
		}
		return object;
	}

	/**
	 * Gets an image from the clipboard
	 *
	 * @return the image, or null if the clipboard is empty, or it doesn't contain an image
	 */
	public static Image getImageFromClipboard()
	{
		var transferable = getTransferable();
		if (transferable != null && transferable.isDataFlavorSupported(DataFlavor.imageFlavor))
		{
			BufferedImage image;
			try
			{
				image = (BufferedImage) transferable.getTransferData(DataFlavor.imageFlavor);
			}
			catch (UnsupportedFlavorException | IOException e)
			{
				throw new RuntimeException(e);
			}
			return SwingFXUtils.toFXImage(image, null);
		}
		return null;
	}

	/**
	 * Copies an image to the clipboard.
	 *
	 * @param image the image to copy to the clipboard
	 */
	public static void copyImageToClipboard(Image image)
	{
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new ImageSelection(SwingFXUtils.fromFXImage(image, null)), null);
	}

	/**
	 * Gets a string from the clipboard.
	 *
	 * @return a string, or null if the clipboard is empty, or it doesn't contain a string
	 */
	public static String getStringFromClipboard()
	{
		var transferable = getTransferable();
		if (transferable != null && transferable.isDataFlavorSupported(DataFlavor.stringFlavor))
		{
			String string;
			try
			{
				string = (String) transferable.getTransferData(DataFlavor.stringFlavor);
			}
			catch (UnsupportedFlavorException | IOException e)
			{
				throw new RuntimeException(e);
			}
			return string;
		}
		return null;
	}

	/**
	 * Copies a string to the clipboard.
	 *
	 * @param text the string to copy to the clipboard
	 */
	public static void copyTextToClipboard(String text)
	{
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
	}

	private static Transferable getTransferable()
	{
		try
		{
			return Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
		}
		catch (HeadlessException | IllegalStateException e)
		{
			log.warn("Clipboard not available: {}", e.getMessage());
			return null;
		}
	}
}
