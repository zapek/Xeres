/*
 * Copyright (c) 2026 by David Gerber - https://zapek.com
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

package io.xeres.app.api.controller.reputation;

import io.xeres.app.api.controller.AbstractControllerTest;
import io.xeres.app.database.model.reputation.ReputationIdentityFakes;
import io.xeres.app.service.ReputationService;
import io.xeres.common.id.GxsId;
import io.xeres.common.reputation.Opinion;
import io.xeres.common.reputation.Reputation;
import io.xeres.common.rest.reputation.ReputationRequest;
import io.xeres.testutils.IdFakes;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;

import static io.xeres.common.rest.PathConfig.REPUTATION_PATH;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReputationController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReputationControllerTest extends AbstractControllerTest
{
	private static final String BASE_URL = REPUTATION_PATH;

	@MockitoBean
	private ReputationService reputationService;

	@Test
	void FindReputationByGxsId_Success() throws Exception
	{
		var reputationIdentity = ReputationIdentityFakes.createReputationIdentity(Opinion.NEUTRAL);
		reputationIdentity.setReputation(Reputation.REMOTELY_POSITIVE);
		reputationIdentity.addOpinion(IdFakes.createLocationIdentifier(), Opinion.POSITIVE);
		reputationIdentity.addOpinion(IdFakes.createLocationIdentifier(), Opinion.POSITIVE);
		reputationIdentity.addOpinion(IdFakes.createLocationIdentifier(), Opinion.NEGATIVE);

		when(reputationService.findByGxsId(reputationIdentity.getGxsId())).thenReturn(Optional.of(reputationIdentity));

		mvc.perform(getJson(BASE_URL + "?gxsId=" + reputationIdentity.getGxsId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.opinion").value(Opinion.NEUTRAL.name()))
				.andExpect(jsonPath("$.reputation").value(Reputation.REMOTELY_POSITIVE.name()))
				.andExpect(jsonPath("$.positiveVotes").value(2))
				.andExpect(jsonPath("$.negativeVotes").value(1));

		verify(reputationService).findByGxsId(reputationIdentity.getGxsId());
	}

	@Test
	void FindReputationByGxsId_Unknown_ReturnsDefault() throws Exception
	{
		var gxsId = IdFakes.createGxsId();

		when(reputationService.findByGxsId(any(GxsId.class))).thenReturn(Optional.empty());

		mvc.perform(getJson(BASE_URL + "?gxsId=" + gxsId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.opinion").value(Opinion.NEUTRAL.name()))
				.andExpect(jsonPath("$.reputation").value(Reputation.NEUTRAL.name()))
				.andExpect(jsonPath("$.positiveVotes").value(0))
				.andExpect(jsonPath("$.negativeVotes").value(0));

		verify(reputationService).findByGxsId(any(GxsId.class));
	}

	@Test
	void SetReputation_Success() throws Exception
	{
		var gxsId = IdFakes.createGxsId();

		mvc.perform(postJson(BASE_URL, new ReputationRequest(gxsId, Opinion.NEGATIVE)))
				.andExpect(status().isNoContent());

		verify(reputationService).updateIdentityReputation(gxsId, Opinion.NEGATIVE);
	}
}
