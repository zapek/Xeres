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

package io.xeres.ui.controller.file;

import io.xeres.ui.client.NotificationClient;
import io.xeres.ui.controller.Controller;
import io.xeres.ui.controller.TabActivation;
import io.xeres.ui.support.util.UiUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedList;

@Component
@FxmlView(value = "/view/file/trend.fxml")
public class FileTrendViewController implements Controller, TabActivation
{
	private static final String NAME_CONTAINS_ALL = "NAME CONTAINS ALL ";
	private static final int MAXIMUM_BACKLOG = 300;
	private static final int MAXIMUM_DUPLICATE_SEARCH = 5;


	private final NotificationClient notificationClient;
	private Disposable notificationDisposable;

	private final ObservableList<TrendResult> trendResult = FXCollections.observableList(new LinkedList<>());

	@FXML
	private TableView<TrendResult> trendTableView;

	// XXX: make sure the table is NOT sortable!!

	@FXML
	private TableColumn<TrendResult, String> tableFrom;

	@FXML
	private TableColumn<TrendResult, String> tableTerms;

	@FXML
	private TableColumn<TrendResult, Instant> tableTime;

	public FileTrendViewController(NotificationClient notificationClient)
	{
		this.notificationClient = notificationClient;
	}

	@Override
	public void initialize() throws IOException
	{
		trendTableView.setItems(trendResult);

		tableTerms.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().keywords()));
		tableFrom.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().senderName()));
		tableTime.setCellFactory(param -> new TimeCell());
		tableTime.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().when()));

		setupFileTrendNotifications();
	}

	private void setupFileTrendNotifications()
	{
		notificationDisposable = notificationClient.getFileTrendNotifications()
				.doOnError(UiUtils::showAlertError)
				.doOnNext(sse -> Platform.runLater(() -> {
					assert sse.data() != null;
					var keywords = sse.data().keywords();

					if (keywords.startsWith(NAME_CONTAINS_ALL))
					{
						keywords = keywords.substring(NAME_CONTAINS_ALL.length());
					}

					// Don't add if it's already in the first few
					// entries. This avoids duplicates.
					if (isAlreadyTrending(keywords))
					{
						return;
					}

					trendResult.addFirst(new TrendResult(keywords, sse.data().senderName(), Instant.now()));
					if (trendTableView.getItems().size() > MAXIMUM_BACKLOG)
					{
						trendTableView.getItems().removeLast();
					}
				}))
				.subscribe();
	}

	private boolean isAlreadyTrending(String keywords)
	{
		return trendResult.stream()
				.limit(MAXIMUM_DUPLICATE_SEARCH)
				.anyMatch(result -> result.keywords().equals(keywords));
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
