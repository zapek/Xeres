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

import javafx.animation.Animation;
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

	public void initialize()
	{
		var t1 = new TranslateTransition(Duration.millis(500), circle1);
		t1.setFromY(0.0f);
		t1.setToY(-12.0f);
		t1.setAutoReverse(true);
		t1.setCycleCount(Animation.INDEFINITE);
		t1.play();

		var t2 = new TranslateTransition(Duration.millis(500), circle2);
		t2.setFromY(0.0f);
		t2.setToY(-12.0f);
		t2.setAutoReverse(true);
		t2.setCycleCount(Animation.INDEFINITE);
		t2.setDelay(Duration.millis(200));
		t2.play();

		var t3 = new TranslateTransition(Duration.millis(500), circle3);
		t3.setFromY(0.0f);
		t3.setToY(-12.0f);
		t3.setAutoReverse(true);
		t3.setCycleCount(Animation.INDEFINITE);
		t3.setDelay(Duration.millis(400));
		t3.play();
	}
}
