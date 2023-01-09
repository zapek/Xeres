package io.xeres.app.api.converter;

import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.BufferedImageHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;

@Component
public class BufferedImageConverter
{
	@Bean
	public HttpMessageConverter<BufferedImage> createBufferedImageHttpMessageConverter()
	{
		return new BufferedImageHttpMessageConverter();
	}
}
