/*
 * Copyright (c) 2019-2023 by David Gerber - https://zapek.com
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

package io.xeres.ui.custom.led;

import javafx.beans.InvalidationListener;
import javafx.scene.control.Skin;
import javafx.scene.control.SkinBase;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.InnerShadow;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

/**
 * A led class. Strongly inspired from Gerrit Grunwald's JavaFXCustomControls <a href="https://github.com/HanSolo/JavaFXCustomControls">JavaFXCustomControls</a>.
 */
public class LedSkin extends SkinBase<LedControl> implements Skin<LedControl>
{
	private static final double PREFERRED_WIDTH = 16;
	private static final double PREFERRED_HEIGHT = 16;

	private static final double MINIMUM_WIDTH = 8;
	private static final double MINIMUM_HEIGHT = 8;

	private static final double MAXIMUM_WIDTH = 1024;
	private static final double MAXIMUM_HEIGHT = 1024;

	public static final String RESIZE_PROPERTY = "RESIZE";
	public static final String COLOR_PROPERTY = "COLOR";
	public static final String STATE_PROPERTY = "STATE";

	private Region frame;
	private Region main;
	private Region highlight;

	private InnerShadow innerShadow;
	private DropShadow glow;

	private LedControl control;

	private final InvalidationListener sizeListener;
	private final InvalidationListener colorListener;
	private final InvalidationListener stateListener;

	public LedSkin(LedControl control)
	{
		super(control);
		this.control = control;
		sizeListener = observable -> handleControlPropertyChanged(RESIZE_PROPERTY);
		colorListener = observable -> handleControlPropertyChanged(COLOR_PROPERTY);
		stateListener = observable -> handleControlPropertyChanged(STATE_PROPERTY);
		initGraphics();
		registerListeners();
	}

	private void initGraphics()
	{
		if (Double.compare(control.getPrefWidth(), 0.0) <= 0 || Double.compare(control.getPrefHeight(), 0.0) <= 0 ||
				Double.compare(control.getWidth(), 0.0) <= 0 || Double.compare(control.getHeight(), 0.0) <= 0)
		{
			if (control.getPrefWidth() > 0 && control.getPrefHeight() > 0)
			{
				control.setPrefSize(control.getPrefWidth(), control.getPrefHeight());
			}
			else
			{
				control.setPrefSize(PREFERRED_WIDTH, PREFERRED_HEIGHT);
			}
		}

		frame = new Region();
		frame.getStyleClass().setAll("frame");

		main = new Region();
		main.getStyleClass().setAll("main");

		innerShadow = new InnerShadow(BlurType.TWO_PASS_BOX, Color.rgb(0, 0, 0, 0.65), 8, 0, 0, 0);

		glow = new DropShadow(BlurType.TWO_PASS_BOX, control.getColor(), 20, 0, 0, 0);
		glow.setInput(innerShadow);

		highlight = new Region();
		highlight.getStyleClass().setAll("highlight");

		getChildren().addAll(frame, main, highlight);
	}

	private void registerListeners()
	{
		control.widthProperty().addListener(sizeListener);
		control.heightProperty().addListener(sizeListener);
		control.colorProperty().addListener(colorListener);
		control.stateProperty().addListener(stateListener);
	}

	@Override
	protected double computeMinWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset)
	{
		return MINIMUM_WIDTH;
	}

	@Override
	protected double computeMinHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset)
	{
		return MINIMUM_HEIGHT;
	}

	@Override
	protected double computeMaxWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset)
	{
		return MAXIMUM_WIDTH;
	}

	@Override
	protected double computeMaxHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset)
	{
		return MAXIMUM_HEIGHT;
	}

	protected void handleControlPropertyChanged(String property)
	{
		if (RESIZE_PROPERTY.equals(property))
		{
			resize();
		}
		else if (COLOR_PROPERTY.equals(property))
		{
			resize();
		}
		else if (STATE_PROPERTY.equals(property))
		{
			main.setEffect(control.hasState() ? glow : innerShadow);
		}
	}

	@Override
	public void dispose()
	{
		control.widthProperty().removeListener(sizeListener);
		control.heightProperty().removeListener(sizeListener);
		control.colorProperty().removeListener(colorListener);
		control.stateProperty().removeListener(stateListener);
		control = null;
	}

	private void resize()
	{
		var width = control.getWidth() - control.getInsets().getLeft() - control.getInsets().getRight();
		var height = control.getHeight() - control.getInsets().getTop() - control.getInsets().getBottom();
		var size = Math.min(width, height);

		if (size > 0)
		{
			innerShadow.setRadius(0.07 * size);
			glow.setRadius(0.36 * size);
			glow.setColor(control.getColor());

			frame.setMaxSize(size, size);

			main.setMaxSize(0.72 * size, 0.72 * size);
			main.relocate(0.14 * size, 0.14 * size);
			main.setEffect(control.hasState() ? glow : innerShadow);

			highlight.setMaxSize(0.58 * size, 0.58 * size);
			highlight.relocate(0.21 * size, 0.21 * size);
		}
	}
}
