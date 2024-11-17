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

package io.xeres.common.pgp;

import io.xeres.common.i18n.I18nEnum;
import io.xeres.common.i18n.I18nUtils;

import java.util.ResourceBundle;

/**
 * This is the trust level for a PGP-like "web of trust" feature. Note that
 * 'undefined' is not here because it's confusing.
 * <p>
 * Note: this is stored in the database in ordinal. Do not modify the order.
 */
public enum Trust implements I18nEnum
{
	/**
	 * No opinion about the trustworthiness of the owner.
	 */
	UNKNOWN,

	/**
	 * No trust about the owner. For example, he's known to sign stuff without
	 * checking or without the other owner's consent.
	 */
	NEVER,

	/**
	 * Trust that the owner doesn't perform certifications blindly but not
	 * very accurately either. Trust will only become valid after multiple certifications (usually 3).
	 * A good default choice.
	 */
	MARGINAL,

	/**
	 * Trust that the owner performs certification very accurately. Trust
	 * will become valid after a single one so use with care.
	 */
	FULL,

	/**
	 * Our own key.
	 */
	ULTIMATE;

	private final ResourceBundle bundle = I18nUtils.getBundle();

	@Override
	public String toString()
	{
		return bundle.getString(getMessageKey(this));
	}
}
