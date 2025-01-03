/*
 * Copyright (c) 2025 by David Gerber - https://zapek.com
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

import io.xeres.common.rest.statistics.RttPeer;
import io.xeres.common.util.ExecutorUtils;
import io.xeres.ui.client.StatisticsClient;
import io.xeres.ui.controller.Controller;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

@Component
@FxmlView(value = "/view/statistics/rtt.fxml")
public class StatisticsRttController implements Controller
{
	private static final int UPDATE_IN_SECONDS = 10;
	private static final int DATA_WINDOW_SIZE = 12; // 2 minutes of data (one each 10 seconds)

	@FXML
	private LineChart<Number, Number> lineChart;

	@FXML
	private NumberAxis xAxis;

	private final Map<Long, XYChart.Series<Number, Number>> peerSeries = new HashMap<>();

	private ScheduledExecutorService executorService;

	private final StatisticsClient statisticsClient;

	private final ResourceBundle bundle;

	public StatisticsRttController(StatisticsClient statisticsClient, ResourceBundle bundle)
	{
		this.statisticsClient = statisticsClient;
		this.bundle = bundle;
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
	}

	public void start()
	{
		executorService = ExecutorUtils.createFixedRateExecutor(() -> statisticsClient.getRttStatistics()
						.doOnSuccess(rttStatisticsResponse -> Platform.runLater(() -> {
							rttStatisticsResponse.peers().forEach(rttPeer -> {
								var series = peerSeries.computeIfAbsent(rttPeer.id(), aLong -> createSeries(rttPeer));
								updateData(series, rttPeer.mean());
							});

							var ids = rttStatisticsResponse.peers().stream()
									.map(RttPeer::id)
									.collect(Collectors.toSet());

							peerSeries.entrySet().removeIf(entry -> {
								if (!ids.contains(entry.getKey()))
								{
									lineChart.getData().remove(entry.getValue());
									return true;
								}
								return false;
							});
						}))
						.subscribe(),
				0,
				UPDATE_IN_SECONDS); // XXX: that period should be shared somewhere
	}

	public void stop()
	{
		ExecutorUtils.cleanupExecutor(executorService);
		peerSeries.clear();
	}

	private XYChart.Series<Number, Number> createSeries(RttPeer rttPeer)
	{
		var series = new XYChart.Series<Number, Number>();
		series.setName(rttPeer.name());
		lineChart.getData().add(series);
		setLegend();
		return series;
	}

	private void setLegend()
	{
		lineChart.lookupAll("Label.chart-legend-item").forEach(node -> {
			if (node instanceof Label label && label.getCursor() == null) // Make sure we only do the job once for each
			{
				label.setCursor(Cursor.HAND);
				label.setOnMouseClicked(event -> {
					label.setOpacity(label.getOpacity() > 0.75 ? 0.5 : 1.0);
					lineChart.getData().forEach(series -> {
						if (series.getName().equals(label.getText()))
						{
							series.getNode().setVisible(!series.getNode().isVisible());
						}
					});
				});
			}
		});
	}

	private static void updateData(XYChart.Series<Number, Number> series, float value)
	{
		series.getData().forEach(numberNumberData -> numberNumberData.setXValue(numberNumberData.getXValue().intValue() - UPDATE_IN_SECONDS));
		series.getData().addFirst(new XYChart.Data<>(0, value));
		if (series.getData().size() > DATA_WINDOW_SIZE + 1)
		{
			series.getData().removeLast();
		}
	}
}
