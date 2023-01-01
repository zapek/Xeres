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

package io.xeres.app.xrs.serialization;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks an item's field as serializable.
 */
@Retention(RUNTIME)
@Target(FIELD)
public @interface RsSerialized
{
	/**
	 * Sets the TLV type, only useful for TLV fields.
	 *
	 * @return the TLV type (default: NONE)
	 */
	TlvType tlvType() default TlvType.NONE;

	/**
	 * Sets the EnumSet's type size.
	 *
	 * @return the EnumSet's type size (default: INTEGER)
	 */
	FieldSize fieldSize() default FieldSize.INTEGER;
}
