/*
 * Copyright (c) 2019-2021 by David Gerber - https://zapek.com
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

package io.xeres.app.web.api;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.xeres.app.web.api.error.Error;
import io.xeres.app.web.api.error.ErrorResponseEntity;
import io.xeres.app.web.api.error.exception.UnprocessableEntityException;
import io.xeres.common.AppName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.persistence.EntityNotFoundException;
import java.net.UnknownHostException;
import java.util.NoSuchElementException;

@RestControllerAdvice
@OpenAPIDefinition(
		info = @Info(
				title = AppName.NAME + " API definition",
				version = "0.1",
				description = "This is the REST API available for UI clients.",
				license = @License(name = "GPL v3", url = "https://www.gnu.org/licenses/gpl-3.0.en.html"),
				contact = @Contact(url = "https://zapek.com", name = "David Gerber", email = "info@zapek.com")
		)
)
public class DefaultHandler
{
	private static final Logger log = LoggerFactory.getLogger(DefaultHandler.class);

	@ExceptionHandler({
			NoSuchElementException.class,
			EntityNotFoundException.class,
			UnknownHostException.class})
	public ResponseEntity<Error> handleNotFoundException(Exception e)
	{
		log.error("Exception: {}, {}", e.getClass().getCanonicalName(), e.getMessage());
		var builder = new ErrorResponseEntity.Builder(HttpStatus.NOT_FOUND)
				.setError("No such entity")
				.setException(e);

		return builder.build();
	}

	@ExceptionHandler(UnprocessableEntityException.class)
	public ResponseEntity<Error> handleUnprocessableEntityException(UnprocessableEntityException e)
	{
		log.error("Exception: {}, {}", e.getClass().getCanonicalName(), e.getMessage());
		return new ErrorResponseEntity.Builder(HttpStatus.UNPROCESSABLE_ENTITY)
				.setError(e.getMessage())
				.setId(e.getId())
				.setException(e.getCause())
				.build();
	}

	@ExceptionHandler(RuntimeException.class)
	public ResponseEntity<Error> handleRuntimeException(RuntimeException e)
	{
		log.error("RuntimeException: {}, {}", e.getClass().getCanonicalName(), e.getMessage(), e);
		return new ErrorResponseEntity.Builder(HttpStatus.INTERNAL_SERVER_ERROR)
				.setError(e.getMessage())
				.setException(e.getCause())
				.build();
	}
}
