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

package io.xeres.app.service.shell;

import java.util.LinkedList;

class History
{
	private final LinkedList<String> historyList = new LinkedList<>();
	private final int maxSize;
	private int currentIndex;

	public History(int maxSize)
	{
		this.maxSize = maxSize;
		currentIndex = -1;
	}

	public void addCommand(String command)
	{
		if (command == null || command.trim().isEmpty())
		{
			return;
		}

		// Remove duplicates
		historyList.remove(command);

		// Add to front
		historyList.addFirst(command);

		// Maintain size
		if (historyList.size() > maxSize)
		{
			historyList.removeLast();
		}

		currentIndex = -1;
	}

	public String getPrevious()
	{
		if (historyList.isEmpty())
		{
			return null;
		}

		if (currentIndex < historyList.size() - 1)
		{
			currentIndex++;
			return historyList.get(currentIndex);
		}

		return historyList.getLast();
	}

	public String getNext()
	{
		if (historyList.isEmpty() || currentIndex <= 0)
		{
			currentIndex = -1;
			return null;
		}

		currentIndex--;
		return historyList.get(currentIndex);
	}
}
