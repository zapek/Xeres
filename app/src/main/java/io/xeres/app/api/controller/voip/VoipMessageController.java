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

package io.xeres.app.api.controller.voip;

import io.xeres.app.xrs.service.voip.VoipRsService;
import io.xeres.common.id.LocationIdentifier;
import io.xeres.common.message.voip.VoipAction;
import io.xeres.common.message.voip.VoipMessage;
import jakarta.validation.Valid;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import static io.xeres.common.message.MessageHeaders.DESTINATION_ID;
import static io.xeres.common.message.MessagePath.VOIP_PRIVATE_DESTINATION;
import static io.xeres.common.message.MessagePath.VOIP_ROOT;

@Controller
@MessageMapping(VOIP_ROOT)
public class VoipMessageController
{
	private final VoipRsService voipRsService;

	public VoipMessageController(VoipRsService voipRsService)
	{
		this.voipRsService = voipRsService;
	}

	@MessageMapping(VOIP_PRIVATE_DESTINATION)
	public void processPrivateVoipMessageFromProducer(@Header(DESTINATION_ID) String destinationId, @Payload @Valid VoipMessage voipMessage)
	{
		var locationIdentifier = LocationIdentifier.fromString(destinationId);

		switch (voipMessage.getAction())
		{
			case VoipAction.RING -> voipRsService.call(locationIdentifier);
			case VoipAction.ACKNOWLEDGE -> voipRsService.accept(locationIdentifier);
			case VoipAction.CLOSE -> voipRsService.hangup(locationIdentifier);
		}
	}
}
