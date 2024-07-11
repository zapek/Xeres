package io.xeres.app.xrs.service.filetransfer;

class ChunkReceiver
{
	private boolean receiving;
	private int chunkNumber;

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
}
