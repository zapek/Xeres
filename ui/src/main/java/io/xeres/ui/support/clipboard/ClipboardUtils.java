/*
 * Copyright (c) 2024 by David Gerber - https://zapek.com
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

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Utility class to use the clipboard. This implementation uses AWT because the clipboard support of JavaFX is, quite frankly, a
 * royal piece of shit.
 * <p>
 * Fails to work with some bitmaps (for example from Telegram, Windows 10 and print screen, Chrome, ...).
 * <p>
 * Fails with data URIs because it tries to find out if the image is a supported format and even though it is, the URL is "wrong" for it.
 */
public final class ClipboardUtils
{
	private ClipboardUtils()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static Image getImageFromClipboard()
	{
		var transferable = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
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

	public static void copyImageToClipboard(Image image)
	{
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new ImageSelection(SwingFXUtils.fromFXImage(image, null)), null);
	}

	public static void copyTextToClipboard(String text)
	{
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
	}
}
