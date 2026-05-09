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

package io.xeres.app.xrs.service.filetransfer;

import io.xeres.app.database.model.location.LocationFakes;
import io.xeres.testutils.Sha1SumFakes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileTransferAgentTest
{
	@Mock
	private FileTransferRsService fileTransferRsService;

	@Mock
	private FileProvider fileProvider;

	@Test
	void processLeecher() throws IOException
	{
		var leecher = LocationFakes.createLocation();
		var hash = Sha1SumFakes.createSha1Sum();

		var agent = new FileTransferAgent(fileTransferRsService, "foo", hash, fileProvider);

		when(fileProvider.getFileSize()).thenReturn(1024L); // Same file size
		when(fileProvider.read(0L, 1024)).thenReturn(new byte[1024]);

		agent.addLeecher(leecher, 0, 1024);
		assertTrue(agent.process());

		verify(fileTransferRsService).sendData(eq(leecher), eq(hash), eq(1024L), eq(0L), any());
	}
}