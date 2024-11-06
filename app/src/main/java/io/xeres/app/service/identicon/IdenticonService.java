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

package io.xeres.app.service.identicon;

import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class IdenticonService
{
	public byte[] getIdenticon(String text)
	{
		var image = generateIdenticon(text, 128, 128);

		var output = new ByteArrayOutputStream();
		try
		{
			ImageIO.write(image, "jpg", output);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
		return output.toByteArray();
	}

	// https://stackoverflow.com/questions/40697056/how-can-i-create-identicons-using-java-or-android
	private BufferedImage generateIdenticon(String text, int imageWidth, int imageHeight)
	{
		int width = 5, height = 5;

		byte[] hash = text.getBytes();

		BufferedImage identicon = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		WritableRaster raster = identicon.getRaster();

		//int[] background = new int[]{255, 255, 255, 0};
		int[] background = new int[]{255 - hash[0] & 255, 255 - hash[1] & 255, 255 - hash[2] & 255, 0};
		int[] foreground = new int[]{hash[0] & 255, hash[1] & 255, hash[2] & 255, 255};

		for (int x = 0; x < width; x++)
		{
			//Enforce horizontal symmetry
			int i = x < 3 ? x : 4 - x;
			for (int y = 0; y < height; y++)
			{
				int[] pixelColor;
				//toggle pixels based on bit being on/off
				if ((hash[i] >> y & 1) == 1)
					pixelColor = foreground;
				else
					pixelColor = background;
				raster.setPixel(x, y, pixelColor);
			}
		}

		//BufferedImage finalImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
		BufferedImage finalImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);

		//Scale image to the size you want
		AffineTransform at = new AffineTransform();
		at.scale((double) imageWidth / width, (double) imageHeight / height);
		AffineTransformOp op = new AffineTransformOp(at, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
		finalImage = op.filter(identicon, finalImage);

		return finalImage;
	}
}
