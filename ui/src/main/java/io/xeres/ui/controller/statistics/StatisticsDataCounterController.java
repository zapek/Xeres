/*
 * Copyright (c) 2025-2026 by David Gerber - https://zapek.com
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

import io.xeres.common.util.ExecutorUtils;
import io.xeres.ui.client.StatisticsClient;
import io.xeres.ui.controller.Controller;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.XYChart;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.stereotype.Component;

import java.util.ResourceBundle;
import java.util.concurrent.ScheduledExecutorService;

@Component
@FxmlView(value = "/view/statistics/datacounter.fxml")
public class StatisticsDataCounterController implements Controller
{
	public static final int UPDATE_IN_SECONDS = 10;

	@FXML
	private BarChart<String, Number> barChart;

	@FXML
	private CategoryAxis xAxis;

	private ScheduledExecutorService executorService;

	private final XYChart.Series<String, Number> in = new XYChart.Series<>();
	private final XYChart.Series<String, Number> out = new XYChart.Series<>();

	private final StatisticsClient statisticsClient;
	private final ResourceBundle bundle;

	public StatisticsDataCounterController(StatisticsClient statisticsClient, ResourceBundle bundle)
	{
		this.statisticsClient = statisticsClient;
		this.bundle = bundle;
	}

	@Override
	public void initialize()
	{
		in.setName(bundle.getString("statistics.turtle.data-in"));
		out.setName(bundle.getString("statistics.turtle.data-out"));
		//noinspection unchecked
		barChart.getData().addAll(in, out);
	}

	public void start()
	{
		executorService = ExecutorUtils.createFixedRateExecutor(() -> statisticsClient.getDataCounterStatistics()
						.doOnSuccess(dataCounterStatisticsResponse -> Platform.runLater(() -> {
							assert dataCounterStatisticsResponse != null;
							in.getData().clear();
							out.getData().clear();

							dataCounterStatisticsResponse.peers().forEach(dataPeer -> {
								in.getData().add(new XYChart.Data<>(dataPeer.name(), dataPeer.received() / 1024));
								out.getData().add(new XYChart.Data<>(dataPeer.name(), dataPeer.sent() / 1024));
							});
						}))
						.subscribe(),
				1,
				UPDATE_IN_SECONDS);
	}

	public void stop()
	{
		ExecutorUtils.cleanupExecutor(executorService);
		barChart.getData().clear();
	}
}
