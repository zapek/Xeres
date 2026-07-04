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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.xeres.app.database.model.reputation.ReputationIdentity;
import io.xeres.app.service.ReputationService;
import io.xeres.common.id.GxsId;
import io.xeres.common.rest.reputation.ReputationRequest;
import io.xeres.common.rest.reputation.ReputationResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import static io.xeres.common.rest.PathConfig.REPUTATION_PATH;

@Tag(name = "Reputation", description = "Reputation")
@RestController
@RequestMapping(value = REPUTATION_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
public class ReputationController
{
	private final ReputationService reputationService;

	public ReputationController(ReputationService reputationService)
	{
		this.reputationService = reputationService;
	}

	@GetMapping
	@Operation(summary = "Gets the reputation of an identity (there's always one, neutral by default)")
	@ApiResponse(responseCode = "200", description = "Request successful")
	public ReputationResponse findReputationByGxsId(@RequestParam(value = "gxsId") String gxsId)
	{
		var gxs = GxsId.fromString(gxsId);
		var reputationIdentity = reputationService.findByGxsId(gxs).orElse(ReputationIdentity.DEFAULT_REPUTATION);
		return new ReputationResponse(reputationIdentity.getOpinion(), reputationIdentity.getReputation(), reputationIdentity.getPositiveVotes(), reputationIdentity.getNegativeVotes());
	}

	@PostMapping
	@Operation(summary = "Updates a reputation. Always works even if the identity is not there yet (we can proactively ban)")
	@ApiResponse(responseCode = "204", description = "Reputation updated successfully")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void setReputation(@RequestBody ReputationRequest request)
	{
		reputationService.updateIdentityReputation(request.gxsId(), request.opinion());
	}
}
