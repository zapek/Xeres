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

package io.xeres.app.api.converter;

import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.BufferedImageHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;

/// Registers a BufferedImageHttpMessageConverter so that when a controller endpoint
/// returns a BufferedImage, it's automatically converted to a common format (PNG, JPEG, etc...).
/// Used by for example the QR code system.
@Component
public class BufferedImageConverter
{
	@Bean
	public HttpMessageConverter<BufferedImage> createBufferedImageHttpMessageConverter()
	{
		return new BufferedImageHttpMessageConverter();
	}
}
