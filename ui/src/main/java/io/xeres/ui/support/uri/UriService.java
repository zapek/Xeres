/*
 * Copyright (c) 2024 by David Gerber - https://zapek.com
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

package io.xeres.ui.support.uri;

import io.xeres.ui.OpenUriEvent;
import io.xeres.ui.support.markdown.UriAction;
import javafx.application.HostServices;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class UriService implements UriAction
{
	private final ApplicationEventPublisher eventPublisher;

	private static HostServices hostServices;

	public UriService(ApplicationEventPublisher eventPublisher)
	{
		this.eventPublisher = eventPublisher;
	}

	public void setHostServices(HostServices hostServices)
	{
		UriService.hostServices = hostServices;
	}

	@Override
	public void openUri(ContentParser contentParser)
	{
		eventPublisher.publishEvent(new OpenUriEvent(contentParser));
	}

	/**
	 * Opens a URI to show in the browser.
	 *
	 * @param uri the URI to open
	 */
	public static void openUri(String uri)
	{
		hostServices.showDocument(uri);
	}
}
