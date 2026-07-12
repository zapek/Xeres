package io.xeres.app.api.converter;

import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.BufferedImageHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;

/**
 * Registers a BufferedImageHttpMessageConverter so that when a controller endpoint
 * returns a BufferedImage, it's automatically converted to a common format (PNG, JPEG, etc...).
 * Used by for example the QR code system.
 */
@Component
public class BufferedImageConverter
{
	@Bean
	public HttpMessageConverter<BufferedImage> createBufferedImageHttpMessageConverter()
	{
		return new BufferedImageHttpMessageConverter();
	}
}
