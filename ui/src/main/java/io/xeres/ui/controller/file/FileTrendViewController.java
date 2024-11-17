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

import io.xeres.ui.client.NotificationClient;
import io.xeres.ui.controller.Controller;
import io.xeres.ui.controller.TabActivation;
import io.xeres.ui.support.util.UiUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;

import java.io.IOException;

@Component
@FxmlView(value = "/view/file/trend.fxml")
public class FileTrendViewController implements Controller, TabActivation
{
	private final NotificationClient notificationClient;
	private Disposable notificationDisposable;

	@FXML
	private TableView<TrendResult> trendTableView;

//	@FXML
//	private TableColumn<TrendResult, Integer> tableHits;
//
@FXML
private TableColumn<TrendResult, String> tableFrom;

	@FXML
	private TableColumn<TrendResult, String> tableTerms;

	public FileTrendViewController(NotificationClient notificationClient)
	{
		this.notificationClient = notificationClient;
	}

	@Override
	public void initialize() throws IOException
	{
		tableTerms.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().keywords()));
		tableFrom.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().senderName()));

		setupFileTrendNotifications();
	}

	private void setupFileTrendNotifications()
	{
		notificationDisposable = notificationClient.getFileTrendNotifications()
				.doOnError(UiUtils::showAlertError)
				.doOnNext(sse -> Platform.runLater(() -> {
					assert sse.data() != null;
					trendTableView.getItems().add(new TrendResult(sse.data().keywords(), sse.data().senderName()));
					if (trendTableView.getItems().size() > 255) // XXX: maybe not optimal...
					{
						trendTableView.getItems().remove(0, 10);
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

	@Override
	public void activate()
	{

	}

	@Override
	public void deactivate()
	{

	}
}
