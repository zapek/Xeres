/*
 * Copyright (c) 2026 by David Gerber - https://zapek.com
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

package io.xeres.ui.support.contentline;

import com.lottie4j.core.model.animation.Animation;
import com.lottie4j.fxplayer.LottiePlayer;
import javafx.scene.Node;
import javafx.scene.paint.Color;

import java.time.Duration;
import java.time.Instant;

public class ContentLottie implements Content
{
	private static final Duration TIME_TO_PLAY = Duration.ofSeconds(30);

	private final LottiePlayer node;
	private Instant start = Instant.EPOCH;

	public ContentLottie(Animation animation)
	{
		node = new LottiePlayer(animation, 128, 128);
		node.setBackgroundColor(Color.TRANSPARENT);
		node.seekToFrame(0.0);
		node.setOnMouseEntered(_ -> playIfPossible());
		node.setOnMouseExited(_ -> stopIfPossible());
	}

	@Override
	public Node getNode()
	{
		return node;
	}

	public void play()
	{
		start = Instant.now();
		loopAnimation();
	}

	public void playOrContinue()
	{
		if (!node.isPlaying())
		{
			play();
		}
	}

	public void stop()
	{
		node.stop();
		node.seekToFrame(0.0);
	}

	private void playIfPossible()
	{
		if (!node.isPlaying())
		{
			node.play();
		}
	}

	private void stopIfPossible()
	{
		if (Duration.between(start, Instant.now()).compareTo(TIME_TO_PLAY) > 0)
		{
			stop();
		}
	}

	private void loopAnimation()
	{
		if (Duration.between(start, Instant.now()).compareTo(TIME_TO_PLAY) <= 0)
		{
			node.playOnceFromStart(_ -> loopAnimation());
		}
		else
		{
			stop();
		}
	}
}
