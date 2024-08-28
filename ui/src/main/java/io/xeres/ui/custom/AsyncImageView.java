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

package io.xeres.ui.custom;

import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
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
 */
public class AsyncImageView extends ImageView
{
	private static final Logger log = LoggerFactory.getLogger(AsyncImageView.class);

	private static final int MAX_RUNNING_TASKS = 4; // same default values as Image's background task loader
	private static int runningTasks;
	private static final Queue<ImageTask> pendingTasks = new LinkedList<>();
	private ImageTask backgroundTask;

	public void loadUrl(String url, Function<String, byte[]> loader)
	{
		backgroundTask = new ImageTask(url, loader);

		synchronized (pendingTasks)
		{
			if (runningTasks >= MAX_RUNNING_TASKS)
			{
				pendingTasks.offer(backgroundTask);
			}
			else
			{
				runningTasks++;
				backgroundTask.start();
			}
		}
	}

	public void cancelLoad()
	{
		if (backgroundTask != null)
		{
			backgroundTask.cancel();
		}
	}

	private final class ImageTask
	{
		private static final ExecutorService BACKGROUND_EXECUTOR = createExecutor();

		private final String url;
		private final Function<String, byte[]> loader;

		private FutureTask<byte[]> future;

		public ImageTask(String url, Function<String, byte[]> loader)
		{
			this.url = url;
			this.loader = loader;
		}

		public void onCompletion(byte[] data)
		{
			if (!ArrayUtils.isEmpty(data))
			{
				setImage(new Image(new ByteArrayInputStream(data)));
			}
			cycleTasks();
		}

		public void onCancel()
		{
			log.debug("Loading canceled");
			cycleTasks();
		}

		public void onException(Exception e)
		{
			log.error("Couldn't load image: {}", e.getMessage());
			cycleTasks();
		}

		public void start()
		{
			future = new FutureTask<>(() -> loader.apply(url))
			{
				@Override
				protected void done()
				{
					Platform.runLater(() -> {
						if (future.isCancelled())
						{
							onCancel();
						}
						else
						{
							try
							{
								onCompletion(future.get());
							}
							catch (InterruptedException e)
							{
								onCancel();
								Thread.currentThread().interrupt();
							}
							catch (ExecutionException e)
							{
								onException(e);
							}
						}
					});
				}
			};
			BACKGROUND_EXECUTOR.execute(future);
		}

		public void cancel()
		{
			future.cancel(true);
		}

		private static ExecutorService createExecutor()
		{
			return Executors.newVirtualThreadPerTaskExecutor();
		}

		private void cycleTasks()
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
