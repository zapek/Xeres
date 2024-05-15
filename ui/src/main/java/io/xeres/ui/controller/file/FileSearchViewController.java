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

package io.xeres.ui.controller.file;

import io.xeres.ui.client.FileClient;
import io.xeres.ui.client.NotificationClient;
import io.xeres.ui.controller.Controller;
import io.xeres.ui.support.util.UiUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import net.rgielen.fxweaver.core.FxmlView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;

import java.io.IOException;

@Component
@FxmlView(value = "/view/file/search.fxml")
public class FileSearchViewController implements Controller
{
	private static final Logger log = LoggerFactory.getLogger(FileSearchViewController.class);

	private final FileClient fileClient;

	@FXML
	private TextField search;

	@FXML
	private TabPane resultTabPane;

	private final NotificationClient notificationClient;
	private Disposable notificationDisposable;

	public FileSearchViewController(FileClient fileClient, NotificationClient notificationClient)
	{
		this.fileClient = fileClient;
		this.notificationClient = notificationClient;
	}

	@Override
	public void initialize() throws IOException
	{
		search.setOnKeyPressed(event -> {
			if (event.getCode().equals(KeyCode.ENTER))
			{
				var searchText = search.getText();
				log.debug("Searching for: {}", searchText);
				search.clear();
				fileClient.search(searchText)
						.doOnSuccess(fileSearchResponse -> Platform.runLater(() -> {
							var fileResultView = new FileResultView(searchText, fileSearchResponse.id());
							resultTabPane.getTabs().add(fileResultView);
						}))
						.subscribe();
			}
		});

		setupFileSearchNotifications();
	}

	private void addToResultTab(int requestId, String name, long size, String hash)
	{
		resultTabPane.getTabs().stream()
				.filter(tab -> ((FileResultView) tab).getSearchId() == requestId)
				.findFirst()
				.ifPresent(tab -> ((FileResultView) tab).addResult(name, size, hash));
	}

	private void setupFileSearchNotifications()
	{
		notificationDisposable = notificationClient.getFileSearchNotifications()
				.doOnError(UiUtils::showAlertError)
				.doOnNext(sse -> Platform.runLater(() -> {
					if (sse.data() != null && sse.data().name() != null)
					{
						addToResultTab(sse.data().requestId(), sse.data().name(), sse.data().size(), sse.data().hash());
					}
				}))
				.subscribe();
	}

	@EventListener
	public void onApplicationEvent(ContextClosedEvent ignored)
	{
		if (notificationDisposable != null && !notificationDisposable.isDisposed())
		{
			notificationDisposable.dispose();
		}
	}
}
