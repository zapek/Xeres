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

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.css.*;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import javafx.scene.paint.Color;

import java.util.List;

/**
 * A led class. Strongly inspired from Gerrit Grunwald's JavaFXCustomControls <a href="https://github.com/HanSolo/JavaFXCustomControls">JavaFXCustomControls</a>.
 */
public class LedControl extends Control
{
	private static final StyleablePropertyFactory<LedControl> FACTORY = new StyleablePropertyFactory<>(Control.getClassCssMetaData());

	// CSS pseudo class
	private static final PseudoClass ON_PSEUDO_CLASS = PseudoClass.getPseudoClass("on");
	private final BooleanProperty state;

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

	public boolean hasState()
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

	public void setStatus(LedStatus ledStatus)
	{
		switch (ledStatus)
		{
			case OK -> setStatusClass("led-status-ok");
			case WARNING -> setStatusClass("led-status-warning");
			case ERROR -> setStatusClass("led-status-error");
		}
	}

	private void setStatusClass(String className)
	{
		getStyleClass().removeAll("led-status-ok", "led-status-warning", "led-status-error");
		getStyleClass().add(className);
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
