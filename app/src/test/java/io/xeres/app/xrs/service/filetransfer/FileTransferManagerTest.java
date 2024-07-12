package io.xeres.app.xrs.service.filetransfer;

import org.junit.jupiter.api.Test;

import java.util.BitSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FileTransferManagerTest
{

	@Test
	void FileTransferManager_toCompressedChunkMap()
	{
		var input = new BitSet(4);
		input.set(0);
		input.set(1);
		input.set(31);
		input.set(32);
		input.set(33);
		input.set(64);

		var output = FileTransferManager.toCompressedChunkMap(input);

		assertEquals(-2147483645, output.getFirst());
		assertEquals(3, output.get(1));
		assertEquals(1, output.get(2));
	}
}