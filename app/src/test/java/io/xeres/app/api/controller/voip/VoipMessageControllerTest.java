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

package io.xeres.app.api.controller.voip;

import io.xeres.app.database.model.location.LocationFakes;
import io.xeres.app.xrs.service.voip.VoipRsService;
import io.xeres.common.message.voip.VoipAction;
import io.xeres.common.message.voip.VoipMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoipMessageControllerTest
{
	private static final String DESTINATION_ID = LocationFakes.createLocation().getLocationIdentifier().toString();

	@Mock
	private VoipRsService voipRsService;

	@Test
	void processPrivateVoipMessageFromProducer_callsCallOnRing()
	{
		var controller = new VoipMessageController(voipRsService);
		var msg = new VoipMessage(VoipAction.RING);

		controller.processPrivateVoipMessageFromProducer(DESTINATION_ID, msg);

		verify(voipRsService, times(1)).call(any());
		verifyNoMoreInteractions(voipRsService);
	}

	@Test
	void processPrivateVoipMessageFromProducer_callsAcceptOnAcknowledge()
	{
		var controller = new VoipMessageController(voipRsService);
		var msg = new VoipMessage(VoipAction.ACKNOWLEDGE);

		controller.processPrivateVoipMessageFromProducer(DESTINATION_ID, msg);

		verify(voipRsService, times(1)).accept(any());
		verifyNoMoreInteractions(voipRsService);
	}

	@Test
	void processPrivateVoipMessageFromProducer_callsHangupOnClose()
	{
		var controller = new VoipMessageController(voipRsService);
		var msg = new VoipMessage(VoipAction.CLOSE);

		controller.processPrivateVoipMessageFromProducer(DESTINATION_ID, msg);

		verify(voipRsService, times(1)).hangup(any());
		verifyNoMoreInteractions(voipRsService);
	}
}
