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

package io.xeres.app.xrs.service.filetransfer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;

/**
 * File transfer class.
 * <p>
 * <img src="doc-files/filetransfer.png" alt="File transfer diagram">
 */
class FileTransferManager implements Runnable
{
	private static final Logger log = LoggerFactory.getLogger(FileTransferManager.class);

	private BlockingQueue<FileTransferCommand> queue;

	public FileTransferManager(BlockingQueue<FileTransferCommand> queue)
	{
		this.queue = queue;
	}

	@Override
	public void run()
	{
		boolean done = false;

		while (!done)
		{
			try
			{
				var command = queue.take();
				processCommand(command);
			}
			catch (InterruptedException e)
			{
				log.debug("FileTransferManager thread interrupted");
				done = true;
				Thread.currentThread().interrupt();
			}
		}
	}

	private void processCommand(FileTransferCommand command)
	{
		log.debug("Processing command {}...", command);
	}
}
