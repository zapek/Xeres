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

package io.xeres.common.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

import java.util.Objects;

public final class ErrorResponseEntity extends ResponseEntity<Error>
{
	private final Error error;

	private ErrorResponseEntity(Error error, HttpStatusCode httpStatusCode)
	{
		super(error, httpStatusCode);
		this.error = error;
	}

	public String getContextId()
	{
		return error.getContextId();
	}

	public String getMessage()
	{
		return error.getMessage();
	}

	public String getDetail()
	{
		return error.getDetails().stream().findFirst().orElse("");
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		var that = (ErrorResponseEntity) o;
		return Objects.equals(error, that.error);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(super.hashCode(), error);
	}

	public static class Builder
	{
		private final HttpStatusCode httpStatusCode;
		private String id;
		private String error;
		private Throwable exception;

		public Builder(HttpStatusCode httpStatusCode)
		{
			this.httpStatusCode = httpStatusCode;
		}

		public Builder setId(String id)
		{
			this.id = id;
			return this;
		}

		public Builder setError(String error)
		{
			this.error = error;
			return this;
		}

		public Builder setException(Throwable exception)
		{
			this.exception = exception;
			return this;
		}

		public ErrorResponseEntity build()
		{
			return new ErrorResponseEntity(new Error(id, error, exception), httpStatusCode);
		}

		public ErrorResponseEntity fromJson(String json)
		{
			var objectMapper = new ObjectMapper();
			try
			{
				return new ErrorResponseEntity(objectMapper.readValue(json, Error.class), httpStatusCode);
			}
			catch (JsonProcessingException e)
			{
				return new ErrorResponseEntity(new Error(null, null, null), httpStatusCode); // XXX: not sure those defaults are the best
			}
		}
	}
}
