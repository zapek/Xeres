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

import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;

import java.time.Duration;

/**
 * Pane showing an intelligent undetermined progress.
 */
public class ProgressPane extends StackPane
{
	private static final Duration PROGRESS_SHOW_DELAY = Duration.ofMillis(250);

	private ProgressIndicator progressIndicator;
	private DelayedAction delayedAction;

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
			delayedAction.run();
		}
		else
		{
			delayedAction.abort();
		}
	}

	private void showProgressIndicator(boolean show)
	{
		getChildrenUnmodifiable().getFirst().setVisible(!show);
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

		delayedAction = new DelayedAction(
				() -> showProgressIndicator(true),
				() -> showProgressIndicator(false),
				PROGRESS_SHOW_DELAY);
	}
}
