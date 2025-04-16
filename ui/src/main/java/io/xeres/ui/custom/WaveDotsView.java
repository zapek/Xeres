/*
 * Copyright (c) 2024-2025 by David Gerber - https://zapek.com
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

import javafx.animation.Animation;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.io.IOException;

public class WaveDotsView extends HBox
{
	@FXML
	private Circle circle1;

	@FXML
	private Circle circle2;

	@FXML
	private Circle circle3;

	public WaveDotsView()
	{
		var loader = new FXMLLoader(getClass().getResource("/view/custom/wavedotsview.fxml"));
		loader.setRoot(this);
		loader.setController(this);

		try
		{
			loader.load();
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	@FXML
	private void initialize()
	{
		var t1 = createAnimation(circle1, Duration.millis(0));
		t1.play();

		var t2 = createAnimation(circle2, Duration.millis(200));
		t2.play();

		var t3 = createAnimation(circle3, Duration.millis(400));
		t3.play();
	}

	private static Animation createAnimation(Circle circle, Duration initialDelay)
	{
		var translate = new TranslateTransition(Duration.millis(300), circle);
		translate.setToY(5.0f);

		var pause = new PauseTransition(Duration.millis(300));

		var sequence = new SequentialTransition(translate, pause);
		sequence.setAutoReverse(true);
		sequence.setCycleCount(Animation.INDEFINITE);
		sequence.setDelay(initialDelay);
		return sequence;
	}
}
