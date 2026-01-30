/*
 * Copyright (c) 2019-2025 by David Gerber - https://zapek.com
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

package io.xeres.app.api;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.xeres.app.api.exception.UnprocessableEntityException;
import io.xeres.common.AppName;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Optional;

@RestControllerAdvice
@OpenAPIDefinition(
		info = @Info(
				title = AppName.NAME,
				version = "1.0",
				summary = "A decentralized and secure application for communication and sharing",
				license = @License(name = "GPL v3", url = "https://www.gnu.org/licenses/gpl-3.0.en.html"),
				contact = @Contact(name = "Xeres", url = "https://xeres.io"),
				description = """
						This is the REST interface for controlling the application. Don't forget to use the _Authorize_ button on the right to enter the same
						credentials as the ones in _Settings / Remote_ (you can cut & paste, don't forget to make the password visible first or it will copy asterisks).
						
						**Note**: because some swagger-ui developers are [braindead](https://github.com/swagger-api/swagger-ui/issues/2030), 64-bit values output are truncated to 53-bit ones.
						"""
		),
		security = @SecurityRequirement(
				name = "api" // Mark all endpoints as authenticated. Otherwise, remove and add @SecurityRequirement(name = "api") separately to all controller classes or methods
		)
)
@SecurityScheme(name = "api", scheme = "basic", type = SecuritySchemeType.HTTP, in = SecuritySchemeIn.HEADER)
public class DefaultHandler extends ResponseEntityExceptionHandler
{
	private static final Logger log = LoggerFactory.getLogger(DefaultHandler.class);
	public static final String TRACE = "trace";

	@ExceptionHandler({
			NoSuchElementException.class,
			EntityNotFoundException.class,
			UnknownHostException.class})
	public ErrorResponse handleNotFoundException(Exception e)
	{
		logError(e, log.isDebugEnabled());
		return ErrorResponse.builder(e, HttpStatus.NOT_FOUND, e.getMessage())
				.property(TRACE, ExceptionUtils.getStackTrace(e))
				.build();
	}

	@ExceptionHandler(UnprocessableEntityException.class)
	public ErrorResponse handleUnprocessableEntityException(UnprocessableEntityException e)
	{
		logError(e, log.isDebugEnabled());
		return ErrorResponse.builder(e, HttpStatus.UNPROCESSABLE_CONTENT, e.getMessage())
				.property(TRACE, ExceptionUtils.getStackTrace(e))
				.build();
	}

	@ExceptionHandler(EntityExistsException.class)
	public ErrorResponse handleEntityExistsException(EntityExistsException e)
	{
		logError(e, log.isDebugEnabled());
		return ErrorResponse.builder(e, HttpStatus.CONFLICT, e.getMessage())
				.property(TRACE, ExceptionUtils.getStackTrace(e))
				.build();
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ErrorResponse handleIllegalArgumentException(IllegalArgumentException e)
	{
		logError(e, log.isDebugEnabled());
		return ErrorResponse.builder(e, HttpStatus.BAD_REQUEST, e.getMessage())
				.property(TRACE, ExceptionUtils.getStackTrace(e))
				.build();
	}

	@ExceptionHandler(Exception.class)
	public ErrorResponse handleException(Exception e)
	{
		logError(e, true);
		return ErrorResponse.builder(e, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage())
				.property(TRACE, ExceptionUtils.getStackTrace(e))
				.build();
	}

	/**
	 * Generates a ResponseStatusException. Those are typically done from media endpoints
	 * and there's no way to put JSON error messages in there, so just ignore them.
	 *
	 * @param e the exception
	 * @return a ResponseEntity with just the status code and no message
	 */
	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<Void> handleResponseStatusException(ResponseStatusException e)
	{
		return new ResponseEntity<>(e.getStatusCode());
	}

	// This one has to use an override
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request)
	{
		var problemDetail = handleValidationException(ex);
		return ResponseEntity.status(status.value()).body(problemDetail);
	}

	private ProblemDetail handleValidationException(MethodArgumentNotValidException ex)
	{
		var details = Optional.of(ex.getDetailMessageArguments())
				.map(args -> Arrays.stream(args)
						.filter(msg -> !ObjectUtils.isEmpty(msg))
						.reduce("Wrong input,", (a, b) -> a + " " + b)
				)
				.orElse("").toString();
		var problemDetail = ProblemDetail.forStatusAndDetail(ex.getStatusCode(), details);
		problemDetail.setInstance(ex.getBody().getInstance());
		return problemDetail;
	}

	private void logError(Exception e, boolean withStackTrace)
	{
		if (withStackTrace)
		{
			log.error("{}: {}", e.getClass().getSimpleName(), e.getMessage(), e);
			return;
		}
		log.error("{}: {}", e.getClass().getSimpleName(), e.getMessage());
	}
}
