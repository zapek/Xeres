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

package io.xeres.ui.controller.contact;

import io.xeres.common.i18n.I18nEnum;
import io.xeres.common.i18n.I18nUtils;
import io.xeres.common.reputation.Opinion;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignE;
import org.kordamp.ikonli.materialdesign2.MaterialDesignT;

enum ContactOpinion implements I18nEnum
{
	POSITIVE(MaterialDesignT.THUMB_UP, "success"),
	NEUTRAL(MaterialDesignE.EMOTICON_NEUTRAL, null),
	NEGATIVE(MaterialDesignT.THUMB_DOWN, "danger");

	ContactOpinion(Ikon icon, String style)
	{
		text = I18nUtils.getBundle().getString(getMessageKey(this));
		this.icon = icon;
		this.style = style;
	}

	private final String text;
	private final Ikon icon;
	private final String style;

	public String getText()
	{
		return text;
	}

	public Ikon getIcon()
	{
		return icon;
	}

	public String getStyle()
	{
		return style;
	}

	public static Opinion toOpinion(ContactOpinion opinion)
	{
		return switch (opinion)
		{
			case POSITIVE -> Opinion.POSITIVE;
			case NEUTRAL -> Opinion.NEUTRAL;
			case NEGATIVE -> Opinion.NEGATIVE;
		};
	}

	public static ContactOpinion fromOpinion(Opinion opinion)
	{
		return switch (opinion)
		{
			case Opinion.POSITIVE -> POSITIVE;
			case Opinion.NEUTRAL -> NEUTRAL;
			case Opinion.NEGATIVE -> NEGATIVE;
		};
	}
}
