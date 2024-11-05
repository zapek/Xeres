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

import io.xeres.common.i18n.I18nUtils;
import io.xeres.ui.OpenUriEvent;
import io.xeres.ui.client.FileClient;
import io.xeres.ui.client.NotificationClient;
import io.xeres.ui.controller.Controller;
import io.xeres.ui.controller.TabActivation;
import io.xeres.ui.support.contextmenu.XContextMenu;
import io.xeres.ui.support.uri.SearchUri;
import io.xeres.ui.support.uri.SearchUriFactory;
import io.xeres.ui.support.util.TextInputControlUtils;
import io.xeres.ui.support.util.UiUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import net.rgielen.fxweaver.core.FxmlView;
import org.apache.commons.lang3.StringUtils;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;

@Component
@FxmlView(value = "/view/file/search.fxml")
public class FileSearchViewController implements Controller, TabActivation
{
	private static final Logger log = LoggerFactory.getLogger(FileSearchViewController.class);

	private static final String COPY_LINK_MENU_ID = "copyLink";

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
	public void initialize()
	{
		TextInputControlUtils.addEnhancedInputContextMenu(search, null);
		search.setOnKeyPressed(event -> {
			if (event.getCode() == KeyCode.ENTER)
			{
				var searchText = search.getText();
				log.debug("Searching for: {}", searchText);
				search.clear();
				fileClient.search(searchText)
						.doOnSuccess(fileSearchResponse -> Platform.runLater(() -> {
							var fileResultView = new FileResultView(fileClient, searchText, fileSearchResponse.id());
							resultTabPane.getTabs().add(fileResultView);
						}))
						.subscribe();
			}
		});

		createContextMenu();
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

	@EventListener
	public void handleOpenUriEvents(OpenUriEvent event)
	{
		if (event.uri() instanceof SearchUri(String keywords))
		{
			search.setText(keywords);
		}
	}

	@Override
	public void activate()
	{

	}

	@Override
	public void deactivate()
	{

	}

	private void createContextMenu()
	{
		var copyLinkItem = new MenuItem(I18nUtils.getString("copy-link"));
		copyLinkItem.setId(COPY_LINK_MENU_ID);
		copyLinkItem.setGraphic(new FontIcon(MaterialDesignL.LINK_VARIANT));
		copyLinkItem.setOnAction(event -> {
			var clipboardContent = new ClipboardContent();
			var fileResultView = (FileResultView) event.getSource();
			clipboardContent.putString(SearchUriFactory.generate(StringUtils.left(fileResultView.getText(), 50), fileResultView.getText()));
			Clipboard.getSystemClipboard().setContent(clipboardContent);
		});

		var xContextMenu = new XContextMenu<Tab>(copyLinkItem);
		xContextMenu.addToNode(resultTabPane);
	}
}
