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

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Class to run a delayed action. Set a start runnable, a stop runnable and delay to run the start runnable.
 */
public class DelayedAction
{
	private final AtomicBoolean shouldRun = new AtomicBoolean();
	private Timeline timeline;
	private final javafx.util.Duration delay;
	private final Runnable start;
	private final Runnable stop;

	public DelayedAction(Runnable start, Runnable stop, Duration delay)
	{
		this.start = start;
		this.stop = stop;
		this.delay = javafx.util.Duration.millis(delay.toMillis());
	}

	/**
	 * Runs the start runnable after a certain delay. If called more than once, the following calls are ignored unless abort() is called first.
	 */
	public void run()
	{
		if (shouldRun.compareAndSet(false, true))
		{
			cleanup();
			var newTimeline = new Timeline(new KeyFrame(delay, event -> {
				if (shouldRun.get())
				{
					start.run();
				}
			}));
			timeline = newTimeline;
			Platform.runLater(newTimeline::play);
		}
	}

	/**
	 * Aborts the start runnable and runs the stop runnable.
	 */
	public void abort()
	{
		if (shouldRun.compareAndSet(true, false))
		{
			cleanup();
			stop.run();
		}
	}

	private void cleanup()
	{
		if (timeline != null)
		{
			timeline.stop();
			timeline = null;
		}
	}
}
