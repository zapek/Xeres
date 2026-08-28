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

package io.xeres.app.service.script;

import io.xeres.common.protocol.http.HttpConstants;
import io.xeres.common.util.ByteUnitUtils;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import static java.net.http.HttpClient.Redirect.NORMAL;

/// Provides a browser-like `fetch()` global to the JavaScript scripts.
///
/// Embedded GraalJS does not ship a `fetch` implementation (only in Node, which we can't access
/// other than from the command line), so Xeres binds one
/// here. It returns a JavaScript Promise that resolves to an object exposing the
/// same surface as the WHATWG `Response`: `ok`, `status`, `text()` and
/// `arrayBuffer()`.
///
/// @link [Using the Fetch API]([...](https://developer.mozilla.org/en-US/docs/Web/API/Fetch_API/Using_Fetch))
/// @link [Fetch API](https://developer.mozilla.org/en-US/docs/Web/API/Fetch_API)
public class JsFetch
{
	private static final Logger log = LoggerFactory.getLogger(JsFetch.class);

	private static final Duration CONNECTION_TIMEOUT = Duration.ofSeconds(10);
	private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
	private static final int DEFAULT_MAX_BYTES = ByteUnitUtils.fromMegabytes(5);

	private final Context context;
	private final Consumer<Runnable> runner;
	private final ExecutorService executorService;
	private final HttpClient httpClient;
	private final Value responseFactory;

	public JsFetch(Context context, Consumer<Runnable> runner)
	{
		this.context = context;
		this.runner = runner;
		// A cached pool with daemon threads performs the blocking HTTP requests. These threads never
		// touch the JS context: the result is marshalled back to the JavaScript runner thread, which is
		// the only thread allowed to access the context (see ScriptService.runOnJsThread).
		executorService = Executors.newCachedThreadPool(runnable ->
		{
			var thread = new Thread(runnable, "JsFetch");
			thread.setDaemon(true);
			return thread;
		});
		httpClient = HttpClient.newBuilder()
				.connectTimeout(CONNECTION_TIMEOUT)
				.followRedirects(NORMAL)
				.build();
		responseFactory = createResponseFactory();
	}

	/// Fetches a URL and returns a JavaScript Promise resolving to a Response-like object.
	///
	/// @param url     the URL to fetch
	/// @param options optional object with `method` (standard), `headers` (standard), `timeoutMs` (extension) and `maxBytes` (extension, default to 5 MB)
	/// @return a Promise
	public Value fetch(Value url, Value options)
	{
		var request = buildRequest(url, options);
		// Read the limit here, on the JS thread, so the worker thread below never touches the options Value.
		int maxBytes = getMaxBytes(options);

		var promiseConstructor = context.getBindings("js").getMember("Promise");
		var executor = context.asValue((ProxyExecutable) args ->
		{
			var resolve = args[0];
			var reject = args[1];
			// Run the blocking HTTP request off the JS thread. Resolving the promise must happen on the
			// JavaScript runner thread, because doing so (responseFactory, createError) accesses the JS
			// context which is confined to that single thread.
			executorService.execute(() ->
			{
				try
				{
					var response = httpClient.send(request, limitedBodyHandler(maxBytes));
					runner.accept(() -> resolve.execute(responseFactory.execute(response.statusCode(), response.body())));
				}
				catch (IOException | RuntimeException e)
				{
					log.warn("Fetch failed for {}: {}", request.uri(), e.getMessage());
					runner.accept(() -> reject.execute(createError(e.getMessage())));
				}
				catch (InterruptedException _)
				{
					Thread.currentThread().interrupt();
					runner.accept(() -> reject.execute(createError("Interrupted")));
				}
			});
			return null;
		});
		return promiseConstructor.newInstance(executor);
	}

	private int getMaxBytes(Value options)
	{
		if (options != null && options.hasMembers())
		{
			var maxBytes = options.getMember("maxBytes");
			if (maxBytes != null && maxBytes.isNumber())
			{
				return maxBytes.asInt();
			}
		}
		return DEFAULT_MAX_BYTES;
	}

	private HttpRequest buildRequest(Value url, Value options)
	{
		var builder = HttpRequest.newBuilder(URI.create(url.asString()))
				.timeout(REQUEST_TIMEOUT)
				.header("User-Agent", HttpConstants.GENERAL_USER_AGENT);

		if (options != null && options.hasMembers())
		{
			var method = options.getMember("method");
			var body = options.getMember("body");
			if (method != null && !method.isNull() && method.isString())
			{
				var m = method.asString().toUpperCase();
				builder.method(m, getBodyPublisher(body));
			}

			var headers = options.getMember("headers");
			if (headers != null && headers.hasMembers())
			{
				for (String key : headers.getMemberKeys())
				{
					var value = headers.getMember(key);
					if (value != null && value.isString())
					{
						builder.header(key, value.asString());
					}
				}
			}

			var timeout = options.getMember("timeoutMs");
			if (timeout != null && timeout.isNumber())
			{
				builder.timeout(Duration.ofMillis(timeout.asLong()));
			}
		}
		return builder.build();
	}

	private HttpRequest.BodyPublisher getBodyPublisher(Value body)
	{
		if (body != null && !body.isNull() && body.isString())
		{
			return HttpRequest.BodyPublishers.ofString(body.asString());
		}
		else
		{
			return HttpRequest.BodyPublishers.noBody();
		}
	}

	private HttpResponse.BodyHandler<byte[]> limitedBodyHandler(int maxLength)
	{
		return responseInfo ->
		{
			var contentLength = responseInfo.headers().firstValue("Content-Length");
			if (contentLength.isPresent())
			{
				try
				{
					if (Long.parseLong(contentLength.get()) > maxLength)
					{
						throw new IllegalArgumentException("Response too large");
					}
				}
				catch (NumberFormatException _)
				{
					log.warn("[JS] [Fetch]: invalid content length");
				}
			}
			return HttpResponse.BodySubscribers.ofByteArray();
		};
	}

	private Value createError(String message)
	{
		var errorConstructor = context.getBindings("js").getMember("Error");
		return errorConstructor.newInstance(message);
	}

	/// Builds a JS function that turns a status and a byte array into a Response-like object.
	private Value createResponseFactory()
	{
		return context.eval("js", """
				(function (status, body) {
					var bytes = new Uint8Array(body);
					var text;
					return {
						ok: status >= 200 && status < 300,
						status: status,
						text: async function () {
							if (text === undefined) {
								text = '';
								for (var i = 0; i < bytes.length; i++) {
									text += String.fromCharCode(bytes[i]);
								}
							}
							return text;
						},
						arrayBuffer: async function () {
							return bytes.buffer;
						}
					};
				})
				""");
	}
}
