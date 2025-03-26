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

package io.xeres.ui.controller.statistics;

import io.xeres.ui.controller.WindowController;
import javafx.fxml.FXML;
import javafx.scene.control.TabPane;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.stereotype.Component;

@Component
@FxmlView(value = "/view/statistics/main.fxml")
public class StatisticsMainWindowController implements WindowController
{
	@FXML
	private TabPane tabPane;

	// This field name to get the controller is some black magic, see last answer at https://stackoverflow.com/questions/40754454/get-controller-instance-from-node
	@FXML
	private StatisticsTurtleController statisticsTurtleController;

	@FXML
	private StatisticsRttController statisticsRttController;

	@FXML
	private StatisticsDataCounterController statisticsDataCounterController;

	@Override
	public void initialize()
	{

	}

	@Override
	public void onShown()
	{
		statisticsTurtleController.start();
		statisticsRttController.start();
		statisticsDataCounterController.start();
	}

	@Override
	public void onHiding()
	{
		statisticsTurtleController.stop();
		statisticsRttController.stop();
		statisticsDataCounterController.stop();
	}
}
