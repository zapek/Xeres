/*
 * Copyright (c) 2024-2026 by David Gerber - https://zapek.com
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

import io.xeres.common.i18n.I18nUtils;
import io.xeres.ui.event.OpenUriEvent;
import io.xeres.ui.support.markdown.UriAction;
import io.xeres.ui.support.preference.PreferenceUtils;
import io.xeres.ui.support.util.Requester;
import javafx.application.HostServices;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;

import static io.xeres.ui.support.preference.PreferenceUtils.MISC;

/**
 * This service is responsible for opening URIs within the application.
 */
@Service
public class UriService implements UriAction
{
	public static final String EXTERNAL_URL_NO_WARNING = "ExternalUrlNoWarning";

	private final ApplicationEventPublisher eventPublisher;
	private final HostServices hostServices;

	public UriService(ApplicationEventPublisher eventPublisher, @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") @Nullable HostServices hostServices)
	{
		this.eventPublisher = eventPublisher;
		this.hostServices = hostServices;
	}

	/**
	 * Opens a URI to show within the application.
	 *
	 * @param uri the URI to open.
	 */
	@Override
	public void openUri(Uri uri)
	{
		eventPublisher.publishEvent(new OpenUriEvent(uri));
	}

	public void showDocument(String uri)
	{
		var preferences = PreferenceUtils.getPreferences().node(MISC);
		if (hostServices != null && StringUtils.isNotBlank(uri))
		{
			if (preferences.getBoolean(EXTERNAL_URL_NO_WARNING, false) ||
					Requester.askTemporarily(MessageFormat.format(I18nUtils.getBundle().getString("requester.external-warning"), StringUtils.abbreviate(uri, 128)), checked -> preferences.putBoolean(EXTERNAL_URL_NO_WARNING, checked)))
			{
				hostServices.showDocument(uri);
			}

		}
	}
}
