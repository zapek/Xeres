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

package io.xeres.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates a program element that is more widely visible than otherwise necessary, only for use by unit test.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.TYPE})
public @interface VisibleForTesting
{
	/**
	 * The visibility the annotated element would have if it did not need
	 * to be made visible for testing.
	 * Default is PRIVATE.
	 */
	int otherwise() default PRIVATE;

	// Constants matching java.lang.reflect.Modifier values for convenience
	int PRIVATE = 2;
	int PACKAGE_PRIVATE = 3;
	int PROTECTED = 4;
	int PUBLIC = 1;

	/**
	 * Indicates the element should never be called from production code,
	 * only from tests.
	 */
	int NONE = 5;
}
