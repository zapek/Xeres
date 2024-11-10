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

import io.xeres.app.configuration.CacheDirConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class IdenticonService
{
	private static final Logger log = LoggerFactory.getLogger(IdenticonService.class);

	private final CacheDirConfiguration cacheDirConfiguration;

	private static final int IMAGE_WIDTH = 128;
	private static final int IMAGE_HEIGHT = 128;

	public IdenticonService(CacheDirConfiguration cacheDirConfiguration)
	{
		this.cacheDirConfiguration = cacheDirConfiguration;
	}

	public byte[] getIdenticon(byte[] hash)
	{
		var data = getIdenticonFromCache(hash);
		if (data != null)
		{
			return data;
		}

		var image = generateIdenticon(hash, IMAGE_WIDTH, IMAGE_HEIGHT);

		var output = new ByteArrayOutputStream();
		try
		{
			ImageIO.write(image, "png", output);
		}
		catch (IOException e)
		{
			throw new RuntimeException("Could not generate identicon", e);
		}
		var outputData = output.toByteArray();
		putIdenticonToCache(hash, outputData);
		return outputData;
	}

	private byte[] getIdenticonFromCache(byte[] hash)
	{
		var path = getFilePath(hash);
		if (path == null)
		{
			return null;
		}

		if (path.toFile().canRead())
		{
			try
			{
				return Files.readAllBytes(path);
			}
			catch (IOException e)
			{
				log.warn("Couldn't read cached file {}: {}", path, e.getMessage());
			}
		}
		return null;
	}

	private void putIdenticonToCache(byte[] hash, byte[] data)
	{
		var path = getFilePath(hash);
		if (path == null)
		{
			return;
		}

		try
		{
			Files.write(path, data);
		}
		catch (IOException e)
		{
			log.warn("Couldn't write cached file {}: {}", path, e.getMessage());
		}
	}

	private Path getFilePath(byte[] hash)
	{
		var cacheDir = cacheDirConfiguration.getCacheDir();
		if (cacheDir == null)
		{
			return null;
		}
		return Path.of(cacheDir, String.format("identicon_%02x%02x%02x", Byte.toUnsignedInt(hash[0]), Byte.toUnsignedInt(hash[1]), Byte.toUnsignedInt(hash[2])));
	}

	/**
	 * Generates an identicon like the ones from GitHub.
	 * <a href="https://github.com/davidhampgonsalves/Contact-Identicons">Android version</a> by David Hamp-Gonsalves.
	 * <a href="https://stackoverflow.com/questions/40697056/how-can-i-create-identicons-using-java-or-android">Java version</a> by Kevin Grandjean.
	 *
	 * @param hash        the hash, at least 3 bytes are needed
	 * @param imageWidth  the width of the images
	 * @param imageHeight the height of the image
	 * @return a buffered image
	 */
	private BufferedImage generateIdenticon(byte[] hash, int imageWidth, int imageHeight)
	{
		assert hash != null && hash.length >= 3;
		var width = 5;
		var height = 5;

		var identicon = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		WritableRaster raster = identicon.getRaster();

		var background = new int[]{240, 240, 240, 255};
		var foreground = new int[]{hash[0] & 255, hash[1] & 255, hash[2] & 255, 255};

		for (var x = 0; x < width; x++)
		{
			//Enforce horizontal symmetry
			int i = x < 3 ? x : 4 - x;
			for (var y = 0; y < height; y++)
			{
				int[] pixelColor;
				//toggle pixels based on bit being on/off
				if ((hash[i] >> y & 1) == 1)
				{
					pixelColor = foreground;
				}
				else
				{
					pixelColor = background;
				}
				raster.setPixel(x, y, pixelColor);
			}
		}

		var finalImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);

		//Scale image to the size you want
		var at = new AffineTransform();
		at.scale((double) imageWidth / width, (double) imageHeight / height);
		var op = new AffineTransformOp(at, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
		finalImage = op.filter(identicon, finalImage);

		return finalImage;
	}
}
