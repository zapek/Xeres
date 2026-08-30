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
import java.time.Duration;
import java.time.Instant;
import java.util.BitSet;
import java.util.Optional;

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
	void seeder_RequestsInitialSliceSize()
	{
		var seeder = LocationFakes.createLocation();
		var hash = Sha1SumFakes.createSha1Sum();

		var agent = new FileTransferAgent(fileTransferRsService, "foo", hash, fileProvider);

		var chunkMap = new BitSet();
		chunkMap.set(0);

		when(fileProvider.getFileSize()).thenReturn((long) FileTransferRsService.CHUNK_SIZE);
		when(fileProvider.getNeededChunk(any())).thenReturn(Optional.of(0));
		when(fileProvider.hasChunk(0)).thenReturn(false);

		agent.addSeeder(seeder);
		agent.addChunkMap(seeder, chunkMap);
		agent.process();

		verify(fileTransferRsService).sendDataRequest(eq(seeder), eq(hash), eq((long) FileTransferRsService.CHUNK_SIZE), eq(0L), eq(FileTransferRsService.getInitialRequestSize()));
	}

	@Test
	void seeder_RampsUpRequestSizeInSlowStart()
	{
		var seeder = LocationFakes.createLocation();
		var hash = Sha1SumFakes.createSha1Sum();

		var agent = new FileTransferAgent(fileTransferRsService, "foo", hash, fileProvider);

		var chunkMap = new BitSet();
		chunkMap.set(0);

		var fileSize = (long) FileTransferRsService.CHUNK_SIZE;
		when(fileProvider.getFileSize()).thenReturn(fileSize);
		when(fileProvider.getNeededChunk(any())).thenReturn(Optional.of(0));
		when(fileProvider.hasChunk(0)).thenReturn(false);

		agent.addSeeder(seeder);
		agent.addChunkMap(seeder, chunkMap);
		agent.process();

		var firstSize = FileTransferRsService.getInitialRequestSize();
		agent.recordReceive(seeder, firstSize);
		agent.process();

		// In slow start with no rate history, the request size doubles on the next slice.
		verify(fileTransferRsService).sendDataRequest(eq(seeder), eq(hash), eq(fileSize), eq((long) firstSize), eq(firstSize * 2));
	}

	@Test
	void seeder_RequestsStayBlockAligned()
	{
		var seeder = LocationFakes.createLocation();
		var hash = Sha1SumFakes.createSha1Sum();

		var agent = new FileTransferAgent(fileTransferRsService, "foo", hash, fileProvider);

		var chunkMap = new BitSet();
		chunkMap.set(0);

		var fileSize = (long) FileTransferRsService.CHUNK_SIZE;
		when(fileProvider.getFileSize()).thenReturn(fileSize);
		when(fileProvider.getNeededChunk(any())).thenReturn(Optional.of(0));
		when(fileProvider.hasChunk(0)).thenReturn(false);

		agent.addSeeder(seeder);
		agent.addChunkMap(seeder, chunkMap);
		agent.process();

		// Measure a rate (~18 KB/s) whose rate cap (9000 bytes) is not a block
		// multiple. The following request must be rounded to a block multiple.
		agent.recordReceive(seeder, 90_000L);
		agent.process();

		agent.recordReceive(seeder, 40_000L);
		agent.process();

		// Offset 49152 (aligned), size 8192 (one block), not the misaligned 9000.
		verify(fileTransferRsService).sendDataRequest(eq(seeder), eq(hash), eq(fileSize), eq(49152L), eq(FileTransferRsService.BLOCK_SIZE));
	}

	@Test
	void seeder_EnrichesPeerOnAdd()
	{
		var seeder = LocationFakes.createLocation();
		var hash = Sha1SumFakes.createSha1Sum();

		var agent = new FileTransferAgent(fileTransferRsService, "foo", hash, fileProvider);

		agent.addSeeder(seeder);

		verify(fileTransferRsService).enrichPeer(any(), eq(seeder));
		verify(fileTransferRsService).sendChunkMapRequest(eq(seeder), eq(hash), eq(false));
	}

	@Test
	void leecher_EnrichesPeerOnAdd() throws IOException
	{
		var leecher = LocationFakes.createLocation();
		var hash = Sha1SumFakes.createSha1Sum();

		var agent = new FileTransferAgent(fileTransferRsService, "foo", hash, fileProvider);

		when(fileProvider.getFileSize()).thenReturn(1024L);
		agent.addLeecher(leecher, 0, 1024);

		verify(fileTransferRsService).enrichPeer(any(), eq(leecher));
	}

	@Test
	void processLeecher() throws IOException
	{
		var leecher = LocationFakes.createLocation();
		var hash = Sha1SumFakes.createSha1Sum();

		var agent = new FileTransferAgent(fileTransferRsService, "foo", hash, fileProvider);

		when(fileProvider.getFileSize()).thenReturn(1024L); // Same file size
		when(fileProvider.read(0L, 1024)).thenReturn(new byte[1024]);

		agent.addLeecher(leecher, 0, 1024);
		agent.process();

		verify(fileTransferRsService).sendData(eq(leecher), eq(hash), eq(1024L), eq(0L), any());
	}

	@Test
	void processLeecher_NextProcessing() throws IOException
	{
		var leecher = LocationFakes.createLocation();
		var hash = Sha1SumFakes.createSha1Sum();

		var agent = new FileTransferAgent(fileTransferRsService, "foo", hash, fileProvider);

		when(fileProvider.getFileSize()).thenReturn(16384L); // Same file size
		when(fileProvider.read(0L, 8192)).thenReturn(new byte[8192]);

		agent.addLeecher(leecher, 0, 16384);
		agent.process();

		assertTrue(agent.getNextProcessing().isAfter(Instant.now()));

		verify(fileTransferRsService).sendData(eq(leecher), eq(hash), eq(16384L), eq(0L), any());
	}

	@Test
	void leecher_UploadCap_PacesByFairShare() throws IOException
	{
		var leecher = LocationFakes.createLocation();
		var hash = Sha1SumFakes.createSha1Sum();

		var agent = new FileTransferAgent(fileTransferRsService, "foo", hash, fileProvider);

		when(fileProvider.getFileSize()).thenReturn(16384L);
		when(fileProvider.read(0L, 8192)).thenReturn(new byte[8192]);
		when(fileTransferRsService.getOwnUploadBandwidthBytesPerSecond()).thenReturn(10_000L);

		var fileLeecher = agent.addLeecher(leecher, 0, 16384);
		fileLeecher.getSendRate().addBytes(100_000L); // Fast peer, measured rate ~20 KB/s

		agent.process();

		// The fair share is ~10 KB/s for a single leecher, so an 8 KB block takes about 820 ms.
		var delay = Duration.between(Instant.now(), agent.getNextProcessing());
		assertTrue(delay.toMillis() >= 800, "Expected a delay of about 820 ms, got " + delay.toMillis());
		assertTrue(delay.toMillis() <= 850, "Expected a delay of about 820 ms, got " + delay.toMillis());
	}
}