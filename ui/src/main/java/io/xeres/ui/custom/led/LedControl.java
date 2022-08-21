/*
 * Copyright (c) 2019-2022 by David Gerber - https://zapek.com
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

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.css.*;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * A led class. Strongly inspired from Gerrit Grunwald's JavaFXCustomControls <a href="https://github.com/HanSolo/JavaFXCustomControls">JavaFXCustomControls</a>.
 */
public class LedControl extends Control
{
	private static final Logger log = LoggerFactory.getLogger(LedControl.class);

	private static final StyleablePropertyFactory<LedControl> FACTORY = new StyleablePropertyFactory<>(Control.getClassCssMetaData());

	// CSS pseudo class
	private static final PseudoClass ON_PSEUDO_CLASS = PseudoClass.getPseudoClass("on");
	private BooleanProperty state;

	// CSS styleable property
	private static final CssMetaData<LedControl, Color> COLOR = FACTORY.createColorCssMetaData("-color", ledControl -> ledControl.color, Color.GREEN, false);
	private final StyleableProperty<Color> color;

	public LedControl()
	{
		getStyleClass().add("led-control");

		state = new BooleanPropertyBase(false)
		{
			@Override
			protected void invalidated()
			{
				pseudoClassStateChanged(ON_PSEUDO_CLASS, get());
			}

			@Override
			public Object getBean()
			{
				return this;
			}

			@Override
			public String getName()
			{
				return "state";
			}
		};

		color = new SimpleStyleableObjectProperty<>(COLOR, this, "color");
	}

	public boolean getState()
	{
		return state.get();
	}

	public void setState(boolean state)
	{
		this.state.set(state);
	}

	public BooleanProperty stateProperty()
	{
		return state;
	}

	public Color getColor()
	{
		return color.getValue();
	}

	public void setColor(Color color)
	{
		this.color.setValue(color);
	}

	@SuppressWarnings("unchecked")
	public ObjectProperty<Color> colorProperty()
	{
		return (ObjectProperty<Color>) color;
	}

	@Override
	protected Skin<?> createDefaultSkin()
	{
		return new LedSkin(this);
	}

	@Override
	protected List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData()
	{
		return FACTORY.getCssMetaData();
	}
}
