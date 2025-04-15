/*
 * Copyright (c) 2024-2025 by David Gerber - https://zapek.com
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

package io.xeres.ui.configuration;

import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.xeres.common.properties.StartupProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;

/**
 * This configuration overrides the default one of Spring Boot by making sure we only use
 * a global webclient. Spring Boot has one that is customized then cloned so that it can only
 * be modified globally once and from a configuration.
 */
@Configuration
public class WebClientConfiguration
{
	@Bean
	public WebClient.Builder webClientBuilder() throws SSLException
	{
		var webClientBuilder = createWebClientBuilder();
		// Allow bigger message sizes (default is 256 KB). Not used yet but potentially
		// a private message can be around 300 KB.
		webClientBuilder.codecs(clientCodecConfigurer -> clientCodecConfigurer.defaultCodecs().maxInMemorySize(300 * 1024));
		return webClientBuilder;
	}

	private WebClient.Builder createWebClientBuilder() throws SSLException
	{
		var useHttps = StartupProperties.getBoolean(StartupProperties.Property.HTTPS, true);

		if (useHttps)
		{

			var sslContext = SslContextBuilder.forClient()
					.trustManager(InsecureTrustManagerFactory.INSTANCE)
					.build();
			var httpClient = HttpClient.create().secure(t -> t.sslContext(sslContext));

			return WebClient.builder().clientConnector(new ReactorClientHttpConnector(httpClient));
		}
		else
		{
			return WebClient.builder();
		}
	}
}
