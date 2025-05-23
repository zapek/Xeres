/*
 * Copyright (c) 2025 by David Gerber - https://zapek.com
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

package io.xeres.ui.client.update;

import java.io.*;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * OutputStream that reports progress.
 */
public class UpdateProgress
{
	public static final int UPDATE_DELAY = 33; // 30 Hz

	private long contentLength = -1;
	private long downloaded;
	private final OutputStream outputStream;

	public UpdateProgress(Path destination, Consumer<UpdateProgress> callback)
	{
		FileOutputStream out;

		try
		{
			out = new FileOutputStream(destination.toFile());
		}
		catch (FileNotFoundException e)
		{
			throw new RuntimeException("File not found: " + destination.toAbsolutePath(), e);
		}

		outputStream = new FilterOutputStream(out)
		{
			private long lastTime;

			@Override
			public void write(byte[] b, int off, int len) throws IOException
			{
				out.write(b, off, len);
				downloaded += len;
				updateStatus();
			}

			@Override
			public void write(int b) throws IOException
			{
				out.write(b);
				downloaded++;
				updateStatus();
			}

			@Override
			public void write(byte[] b) throws IOException
			{
				out.write(b);
				downloaded += b.length;
				updateStatus();
			}

			private void updateStatus() throws IOException
			{
				var now = System.currentTimeMillis();
				if (now - lastTime > UPDATE_DELAY || downloaded == contentLength)
				{
					if (downloaded == contentLength)
					{
						close();
					}
					callback.accept(UpdateProgress.this);
					lastTime = now;
				}
			}
		};
	}

	public void setContentLength(long contentLength)
	{
		this.contentLength = contentLength;
	}

	public OutputStream getOutputStream()
	{
		return outputStream;
	}

	public double getProgress()
	{
		if (contentLength == -1)
		{
			return 0;
		}
		return downloaded / (double) contentLength;
	}

	public long getDownloaded()
	{
		return downloaded;
	}
}
