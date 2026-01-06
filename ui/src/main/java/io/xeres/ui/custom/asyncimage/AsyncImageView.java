/*
 * Copyright (c) 2024-2026 by David Gerber - https://zapek.com
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

package io.xeres.ui.custom.asyncimage;

import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.function.Function;

/**
 * An {@link ImageView} subclass that can load images asynchronously like {@link Image} does with
 * its argument constructor. The difference is that this class can use any function for doing so
 * and not just load from a public URL.
 * <p>
 * Important: always use {@link #updateImage} instead of {@link #setImage} (which is final).
 */
public class AsyncImageView extends ImageView
{
	private static final Logger log = LoggerFactory.getLogger(AsyncImageView.class);

	private static final int MAX_RUNNING_TASKS = 4; // same default values as Image's background task loader
	private static int runningTasks;
	private static final Queue<LoaderTask> pendingTasks = new LinkedList<>();

	private WeakReference<LoaderTask> taskReference;
	private String url;
	private Function<String, byte[]> loader;
	private Runnable onSuccess;
	private ImageCache imageCache;
	private boolean canCallSetImage;

	public AsyncImageView()
	{
		this(null, null);
	}

	public AsyncImageView(Function<String, byte[]> loader)
	{
		this(loader, null);
	}

	public AsyncImageView(Function<String, byte[]> loader, ImageCache imageCache)
	{
		super();
		setLoader(loader);
		setImageCache(imageCache);
		// setImage() is final and the listener is called *after* the
		// property is set (and acted upon by ImageView) so this is the
		// next best thing we can do.
		imageProperty().addListener((_, _, _) -> {
			if (!canCallSetImage)
			{
				var sb = new StringBuilder("setImage() has been called on AsyncImageView! This can cause problems like images being empty or wrong. Use updateImage() instead!\n");
				var trace = Thread.currentThread().getStackTrace();
				for (var stackTraceElement : trace)
				{
					sb.append("\tat ").append(stackTraceElement).append("\n");
				}
				log.error(sb.toString());
			}
		});
	}

	/**
	 * Sets the url to load. Also accepts file: urls (in that case the loader is bypassed).
	 *
	 * @param url the url to load
	 */
	public void setUrl(String url)
	{
		if (StringUtils.isBlank(url))
		{
			cancel();
			this.url = null;
			updateImage(null);
		}
		else
		{
			if (getImage() != null)
			{
				if (this.url != null && this.url.equals(url))
				{
					// Do not load again, if it's already loaded/being loaded.
					if (onSuccess != null)
					{
						onSuccess.run();
					}
					return;
				}
				updateImage(null);
			}
			this.url = url;
			LoaderTask.loadImage(this, url, loader, onSuccess, imageCache);
		}
	}

	/**
	 * Sets the image. <b>HAS</b> to be used instead of {@link #setImage} otherwise there
	 * might be side effects like missing images or wrong image.
	 *
	 * @param image the image, can be null
	 */
	public void updateImage(Image image)
	{
		setLoaderTask(null);
		canCallSetImage = true;
		setImage(image);
		canCallSetImage = false;
	}

	public void setLoader(Function<String, byte[]> loader)
	{
		this.loader = loader;
	}

	public void setOnSuccess(Runnable onSuccess)
	{
		this.onSuccess = onSuccess;
	}

	public void setImageCache(ImageCache imageCache)
	{
		this.imageCache = imageCache;
	}

	public void cancel()
	{
		var task = getLoaderTask();
		if (task != null)
		{
			task.cancel();
		}
	}

	private void setLoaderTask(LoaderTask task)
	{
		if (task == null)
		{
			if (taskReference != null)
			{
				taskReference.clear();
			}
		}
		else
		{
			taskReference = new WeakReference<>(task);
		}
	}

	private LoaderTask getLoaderTask()
	{
		if (taskReference != null)
		{
			return taskReference.get();
		}
		return null;
	}

	private static final class LoaderTask
	{
		private static final ExecutorService BACKGROUND_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

		private final WeakReference<AsyncImageView> imageViewReference;
		private final String url;
		private final Runnable onSuccess;
		private final ImageCache imageCache;

		private final FutureTask<byte[]> future;

		private static void loadImage(AsyncImageView imageView, String url, Function<String, byte[]> loader, Runnable onSuccess, ImageCache imageCache)
		{
			if (useFromCache(url, imageView, imageCache))
			{
				if (onSuccess != null)
				{
					onSuccess.run();
				}
				return;
			}
			if (canDoWork(url, imageView))
			{
				var task = new LoaderTask(imageView, url, loader, onSuccess, imageCache);
				imageView.setLoaderTask(task);
				runTask(task);
			}
		}

		private static boolean canDoWork(String url, AsyncImageView imageView)
		{
			var task = getLoaderTask(imageView);

			if (task != null)
			{
				if (url.equals(task.url))
				{
					// Same work already in progress
					return false;
				}
				else
				{
					task.cancel();
				}
			}
			return true;
		}

		private static boolean useFromCache(String url, AsyncImageView imageView, ImageCache imageCache)
		{
			if (imageCache != null)
			{
				var image = imageCache.getImage(url);
				if (image != null)
				{
					imageView.updateImage(image);
					return true;
				}
			}
			return false;
		}

		private LoaderTask(AsyncImageView asyncImageView, String url, Function<String, byte[]> loader, Runnable onSuccess, ImageCache imageCache)
		{
			imageViewReference = new WeakReference<>(asyncImageView);
			this.url = url;
			this.onSuccess = onSuccess;
			this.imageCache = imageCache;
			future = new FutureTask<>(() -> isFileUri(url) ? loadFile(url) : loader.apply(url))
			{
				@Override
				protected void done()
				{
					if (future.isCancelled())
					{
						onCancel();
					}
					else
					{
						try
						{
							var data = future.get();
							if (ArrayUtils.isEmpty(data))
							{
								onFailure();
							}
							else
							{
								// Image can apparently decode outside the main thread, which is exactly what we need.
								onCompletion(decodeImage(data));
							}
						}
						catch (InterruptedException _)
						{
							onCancel();
							Thread.currentThread().interrupt();
						}
						catch (ExecutionException e)
						{
							onException(e);
						}
					}
				}
			};
		}

		private static boolean isFileUri(String url)
		{
			try
			{
				var uri = new URI(url);
				return "file".equalsIgnoreCase(uri.getScheme());
			}
			catch (URISyntaxException _)
			{
				return false;
			}
		}

		private static byte[] loadFile(String url)
		{
			try
			{
				var path = Paths.get(new URI(url));
				return Files.readAllBytes(path);
			}
			catch (IOException | URISyntaxException e)
			{
				throw new RuntimeException(e);
			}
		}

		private Image decodeImage(byte[] data)
		{
			return new Image(new ByteArrayInputStream(data));
		}

		private void onCompletion(Image image)
		{
			if (!future.isCancelled())
			{
				Platform.runLater(() -> {
					if (imageCache != null)
					{
						imageCache.putImage(url, image);
					}
					var imageView = imageViewReference.get();
					runIfSameTask(imageView, () -> {
						assert imageView != null;
						imageView.updateImage(image);
						if (onSuccess != null)
						{
							onSuccess.run();
						}
					});
				});
				cycleTasks();
			}
		}

		private void onFailure()
		{
			cycleTasks();
		}

		private void onCancel()
		{
			cycleTasks();
		}

		private void onException(Exception e)
		{
			log.error("Couldn't load image: {}", e.getMessage());
			cycleTasks();
		}

		private void runIfSameTask(AsyncImageView imageView, Runnable runnable)
		{
			var task = getLoaderTask(imageView);
			if (this == task)
			{
				runnable.run();
			}
		}

		private void start()
		{
			BACKGROUND_EXECUTOR.execute(future);
		}

		private void cancel()
		{
			future.cancel(true);
		}

		private static LoaderTask getLoaderTask(AsyncImageView imageView)
		{
			if (imageView != null)
			{
				return imageView.getLoaderTask();
			}
			return null;
		}

		private static void runTask(LoaderTask task)
		{
			synchronized (pendingTasks)
			{
				if (runningTasks >= MAX_RUNNING_TASKS)
				{
					pendingTasks.offer(task);
				}
				else
				{
					runningTasks++;
					task.start();
				}
			}
		}

		private static void cycleTasks()
		{
			synchronized (pendingTasks)
			{
				runningTasks--;
				var nextTask = pendingTasks.poll();
				if (nextTask != null)
				{
					runningTasks++;
					nextTask.start();
				}
			}
		}
	}
}
