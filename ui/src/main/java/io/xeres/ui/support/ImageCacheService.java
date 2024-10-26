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

package io.xeres.ui.support;

import io.xeres.ui.custom.asyncimage.ImageCache;
import io.xeres.ui.properties.UiClientProperties;
import javafx.scene.image.Image;
import org.springframework.stereotype.Service;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.LinkedHashMap;

/**
 * Image cache service. Can only be used on one thread (normally the JavaFX thread).
 */
@Service
public class ImageCacheService implements ImageCache
{
	/**
	 * The maximum size for one image to be allowed in the cache.
	 */
	private static final int MAX_IMAGE_SIZE = 300 * 300 * 4;

	private final LinkedHashMap<String, ImageSizeSoftReference> images = new LinkedHashMap<>(16, 0.75f, true);
	private final int maxSize;
	private int currentSize;
	private final ReferenceQueue<Image> referenceQueue = new ReferenceQueue<>();

	public ImageCacheService(UiClientProperties uiClientProperties)
	{
		maxSize = uiClientProperties.getImageCacheSize() * 1024;
	}

	@Override
	public Image getImage(String url)
	{
		var ref = images.get(url);
		if (ref != null)
		{
			return ref.get();
		}
		return null;
	}

	@Override
	public void putImage(String url, Image image)
	{
		if (!isImageCacheable(image))
		{
			return;
		}

		// Already in there
		var ref = images.get(url);
		if (ref != null && ref.get() == image)
		{
			return;
		}
		// Old entry, remove it
		if (ref != null)
		{
			removeRef(ref);
		}

		int newSize = (int) image.getWidth() * (int) image.getHeight() * 4;
		currentSize += newSize;

		cleanupOldReferencesIfNeeded();
		cleanupOldItemsIfNeeded();

		images.put(url, new ImageSizeSoftReference(image, referenceQueue, url, newSize));
	}

	@Override
	public void evictImage(String url)
	{
		var ref = images.get(url);
		if (ref != null)
		{
			removeRef(ref);
		}
	}

	@Override
	public void evictAllImages()
	{
		images.clear();
		currentSize = 0;
	}

	private void removeRef(ImageSizeSoftReference ref)
	{
		currentSize -= ref.size;
		images.remove(ref.url);
	}

	private void cleanupOldReferencesIfNeeded()
	{
		ImageSizeSoftReference ref;
		if (currentSize > maxSize)
		{
			while ((ref = (ImageSizeSoftReference) referenceQueue.poll()) != null)
			{
				images.remove(ref.url);
				currentSize -= ref.size;
			}
		}
	}

	private void cleanupOldItemsIfNeeded()
	{
		if (currentSize > maxSize)
		{
			var it = images.entrySet().iterator();
			while ((currentSize > maxSize) && (it.hasNext()))
			{
				var entry = it.next();
				it.remove();
				currentSize -= entry.getValue().size;
			}
		}
	}

	private boolean isImageCacheable(Image image)
	{
		return maxSize > 0 && image.getWidth() * image.getHeight() * 4 < MAX_IMAGE_SIZE;
	}

	private static class ImageSizeSoftReference extends SoftReference<Image>
	{
		private final String url;
		private final int size;

		public ImageSizeSoftReference(Image referent, ReferenceQueue<? super Image> q, String url, int size)
		{
			super(referent, q);
			this.url = url;
			this.size = size;
		}
	}
}
