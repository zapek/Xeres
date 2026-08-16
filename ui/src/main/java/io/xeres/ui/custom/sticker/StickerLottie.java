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

package io.xeres.ui.custom.sticker;

import com.lottie4j.core.model.animation.Animation;
import com.lottie4j.fxplayer.LottiePlayer;
import io.xeres.ui.support.util.LottieUiUtils;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.Objects;

class StickerLottie implements Sticker
{
	private static final Logger log = LoggerFactory.getLogger(StickerLottie.class);

	private final Path filePath;
	private Animation animation;

	public StickerLottie(Path filePath)
	{
		this.filePath = filePath;
		try
		{
			animation = LottieUiUtils.decodeLottie(new FileInputStream(filePath.toFile()));
		}
		catch (FileNotFoundException e)
		{
			log.debug("Couldn't open lottie, file not found: {}", e.getMessage());
		}
	}

	@Override
	public boolean hasNode()
	{
		return animation != null;
	}

	@Override
	public Node createMainNode(Node parent)
	{
		Objects.requireNonNull(animation);

		var player = new LottiePlayer(animation, 32, 32);
		player.setBackgroundColor(Color.TRANSPARENT);
		player.seekToFrame(0.0);
		return player;
	}

	@Override
	public Node createNode(Node parent)
	{
		Objects.requireNonNull(animation);

		var player = new LottiePlayer(animation, 128, 128);
		player.setBackgroundColor(Color.TRANSPARENT);
		player.setAdaptiveOffscreenScalingEnabled(true);
		player.seekToFrame(0.0);
		player.setUserData(filePath);
		player.setOnMouseEntered(_ -> player.play());
		player.setOnMouseExited(_ -> {
			player.stop();
			player.seekToFrame(0.0);
		});
		return player;
	}
}
