package io.xeres.app.xrs.service.filetransfer;

import java.util.BitSet;

class ChunkReceiver
{
	private boolean receiving;
	private int chunkNumber;
	private BitSet chunkMap;

	public boolean isReceiving()
	{
		return receiving;
	}

	public void setReceiving(boolean receiving)
	{
		this.receiving = receiving;
	}

	public int getChunkNumber()
	{
		return chunkNumber;
	}

	public void setChunkNumber(int chunkNumber)
	{
		this.chunkNumber = chunkNumber;
	}

	public boolean hasChunkMap()
	{
		return chunkMap != null;
	}

	public BitSet getChunkMap()
	{
		return chunkMap;
	}

	public void setChunkMap(BitSet chunkMap)
	{
		this.chunkMap = chunkMap;
	}
}
