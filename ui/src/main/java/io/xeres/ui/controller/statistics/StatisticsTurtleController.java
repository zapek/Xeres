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

package io.xeres.ui.controller.statistics;

import io.xeres.common.util.NoSuppressedRunnable;
import io.xeres.ui.client.StatisticsClient;
import io.xeres.ui.controller.Controller;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import net.rgielen.fxweaver.core.FxmlView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@FxmlView(value = "/view/statistics/turtle.fxml")
public class StatisticsTurtleController implements Controller
{
	private static final int UPDATE_IN_SECONDS = 2;
	private static final int DATA_WINDOW_SIZE = 60; // 2 minutes of data (one data each 2 seconds)
	private static final Logger log = LoggerFactory.getLogger(StatisticsTurtleController.class);

	@FXML
	private LineChart<Number, Number> lineChart;

	@FXML
	private NumberAxis xAxis;

	private ScheduledExecutorService scheduledExecutorService;

	private final StatisticsClient statisticsClient;

	private final XYChart.Series<Number, Number> dataDownload = new XYChart.Series<>();
	private final XYChart.Series<Number, Number> dataUpload = new XYChart.Series<>();
	private final XYChart.Series<Number, Number> forwardTotal = new XYChart.Series<>();
	private final XYChart.Series<Number, Number> tunnelRequestsDownload = new XYChart.Series<>();
	private final XYChart.Series<Number, Number> tunnelRequestsUpload = new XYChart.Series<>();
	private final XYChart.Series<Number, Number> searchRequestsDownload = new XYChart.Series<>();
	private final XYChart.Series<Number, Number> searchRequestsUpload = new XYChart.Series<>();

	public StatisticsTurtleController(StatisticsClient statisticsClient)
	{
		this.statisticsClient = statisticsClient;
	}

	@Override
	public void initialize() throws IOException
	{
		xAxis.setTickLabelFormatter(new NumberAxis.DefaultFormatter(xAxis)
		{
			@Override
			public String toString(Number object)
			{
				return String.valueOf(-object.intValue());
			}
		});


		dataDownload.setName("Data download");
		dataUpload.setName("Data upload");
		forwardTotal.setName("Data forward");
		tunnelRequestsDownload.setName("Tunnel requests download");
		tunnelRequestsUpload.setName("Tunnel requests upload");
		searchRequestsDownload.setName("Search requests download");
		searchRequestsUpload.setName("Search requests upload");

		lineChart.getData().add(dataDownload);
		lineChart.getData().add(dataUpload);
		lineChart.getData().add(forwardTotal);
		lineChart.getData().add(tunnelRequestsDownload);
		lineChart.getData().add(tunnelRequestsUpload);
		lineChart.getData().add(searchRequestsDownload);
		lineChart.getData().add(searchRequestsUpload);
	}

	public void start()
	{
		scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
		scheduledExecutorService.scheduleAtFixedRate((NoSuppressedRunnable) () -> {
			statisticsClient.getStatistics()
					.doOnSuccess(turtleStatisticsResponse -> Platform.runLater(() -> {
						updateData(dataDownload, turtleStatisticsResponse.dataDownload() / 1024f);
						updateData(dataUpload, turtleStatisticsResponse.dataUpload() / 1024f);
						updateData(forwardTotal, turtleStatisticsResponse.forwardTotal() / 1024f);
						updateData(tunnelRequestsDownload, turtleStatisticsResponse.tunnelRequestsDownload() / 1024f);
						updateData(tunnelRequestsUpload, turtleStatisticsResponse.tunnelRequestsUpload() / 1024f);
						updateData(searchRequestsDownload, turtleStatisticsResponse.searchRequestsDownload() / 1024f);
						updateData(searchRequestsUpload, turtleStatisticsResponse.searchRequestsUpload() / 1024f);
					}))
					.subscribe();
		}, 0, UPDATE_IN_SECONDS, TimeUnit.SECONDS); // XXX: that period should be shared somewhere
	}

	public void stop()
	{
		scheduledExecutorService.shutdownNow();
		try
		{
			var success = scheduledExecutorService.awaitTermination(1, TimeUnit.SECONDS);
			if (!success)
			{
				log.warn("Executor failed to terminate during the waiting period");
			}
		}
		catch (InterruptedException ignored)
		{
			Thread.currentThread().interrupt();
		}
	}

	private void updateData(XYChart.Series<Number, Number> series, float value)
	{
		series.getData().forEach(numberNumberData -> numberNumberData.setXValue(numberNumberData.getXValue().intValue() - UPDATE_IN_SECONDS));
		series.getData().addFirst(new XYChart.Data<>(0, value));
		if (series.getData().size() > DATA_WINDOW_SIZE + 1)
		{
			series.getData().removeLast();
		}
	}
}
