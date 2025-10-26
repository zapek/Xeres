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

package io.xeres.ui.controller.voip;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.time.Instant;
import java.util.function.Consumer;

public class TimeCounter
{
	private Instant startTime;
	private Timeline timeline;
	private final Consumer<java.time.Duration> consumer;

	public TimeCounter(Consumer<java.time.Duration> consumer)
	{
		this.consumer = consumer;
	}

	public void start()
	{
		stop();
		startTime = Instant.now();
		timeline = new Timeline(
				new KeyFrame(Duration.ZERO, _ -> update()),
				new KeyFrame(Duration.seconds(1))
		);
		timeline.setCycleCount(Animation.INDEFINITE);
		timeline.play();
	}

	private void update()
	{
		var now = Instant.now();
		var duration = java.time.Duration.between(startTime, now);
		consumer.accept(duration);
	}

	public void stop()
	{
		if (timeline != null)
		{
			timeline.stop();
			update();
			timeline = null;
		}
	}
}
