/*
 * Copyright (c) 2023 by David Gerber - https://zapek.com
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
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Pane showing an intelligent undetermined progress.
 */
public class ProgressPane extends StackPane
{
	private static final double PROGRESS_SHOW_DELAY_MILLISECONDS = 250.0;

	private ProgressIndicator progressIndicator;
	private final AtomicBoolean shouldShow = new AtomicBoolean();
	private Timeline timeline;

	/**
	 * Shows the progress, but only after a certain delay, to avoid UI flickering in case the progress is quick.
	 *
	 * @param show {@code true} to show the progress, {@code false} to remove it.
	 */
	public void showProgress(boolean show)
	{
		setupProgressIndicatorIfNeeded();

		if (show)
		{
			if (shouldShow.compareAndSet(false, true))
			{
				runDelayed(() -> showProgressIndicator(true));
			}
		}
		else
		{
			if (shouldShow.compareAndSet(true, false))
			{
				removeDelayed();
			}
		}
	}

	private void showProgressIndicator(boolean show)
	{
		getChildrenUnmodifiable().get(0).setVisible(!show);
		progressIndicator.setVisible(show);
	}

	private void setupProgressIndicatorIfNeeded()
	{
		if (progressIndicator != null)
		{
			return;
		}

		if (getChildrenUnmodifiable().size() == 1)
		{
			progressIndicator = new ProgressIndicator();
			progressIndicator.setVisible(false);
			getChildren().add(progressIndicator);
		}
		else
		{
			throw new IllegalStateException("Progress indicator is only supported if there's 1 children");
		}
	}

	private void runDelayed(Runnable runnable)
	{
		removeDelayed();
		timeline = new Timeline(new KeyFrame(Duration.millis(PROGRESS_SHOW_DELAY_MILLISECONDS), event -> {
			if (shouldShow.get())
			{
				runnable.run();
			}
		}));
		Platform.runLater(() -> timeline.play());
	}

	private void removeDelayed()
	{
		if (timeline != null)
		{
			timeline.stop();
			timeline = null;
			showProgressIndicator(false);
		}
	}
}
