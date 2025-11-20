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

package io.xeres.app.api.controller.statistics;

import io.xeres.app.api.controller.AbstractControllerTest;
import io.xeres.app.xrs.service.bandwidth.BandwidthRsService;
import io.xeres.app.xrs.service.rtt.RttRsService;
import io.xeres.app.xrs.service.turtle.TurtleRsService;
import io.xeres.app.xrs.service.turtle.TurtleStatistics;
import io.xeres.common.rest.statistics.DataCounterPeer;
import io.xeres.common.rest.statistics.DataCounterStatisticsResponse;
import io.xeres.common.rest.statistics.RttPeer;
import io.xeres.common.rest.statistics.RttStatisticsResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static io.xeres.common.rest.PathConfig.STATISTICS_PATH;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StatisticsController.class)
@AutoConfigureMockMvc(addFilters = false)
class StatisticsControllerTest extends AbstractControllerTest
{
	private static final String BASE_URL = STATISTICS_PATH;

	@MockitoBean
	private TurtleRsService turtleRsService;

	@MockitoBean
	private RttRsService rttRsService;

	@MockitoBean
	private BandwidthRsService bandwidthRsService;

	@Test
	void GetTurtleStatistics_Success() throws Exception
	{
		var stats = new TurtleStatistics();
		stats.addToDataDownload(5);
		when(turtleRsService.getStatistics()).thenReturn(stats);

		mvc.perform(getJson(BASE_URL + "/turtle"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dataDownload").value(is(5.0f), Float.class));

		verify(turtleRsService).getStatistics();
	}

	@Test
	void GetRttStatistics_Success() throws Exception
	{
		var rttPeer = new RttPeer(1L, "foo", 2);
		var stats = new RttStatisticsResponse(List.of(rttPeer));
		when(rttRsService.getStatistics()).thenReturn(stats);

		mvc.perform(getJson(BASE_URL + "/rtt"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.peers.[0].id").value(is(1L), Long.class))
				.andExpect(jsonPath("$.peers.[0].name").value(is("foo"), String.class))
				.andExpect(jsonPath("$.peers.[0].mean").value(is(2L), Long.class));

		verify(rttRsService).getStatistics();
	}

	@Test
	void GetDataCounterStatistics_Success() throws Exception
	{
		var dataCounterPeer = new DataCounterPeer(1L, "foo", 2L, 3L);
		var stats = new DataCounterStatisticsResponse(List.of(dataCounterPeer));
		when(bandwidthRsService.getDataCounterStatistics()).thenReturn(stats);

		mvc.perform(getJson(BASE_URL + "/data-counter"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.peers.[0].id").value(is(1L), Long.class))
				.andExpect(jsonPath("$.peers.[0].name").value(is("foo"), String.class))
				.andExpect(jsonPath("$.peers.[0].sent").value(is(2L), Long.class))
				.andExpect(jsonPath("$.peers.[0].received").value(is(3L), Long.class));

		verify(bandwidthRsService).getDataCounterStatistics();
	}
}