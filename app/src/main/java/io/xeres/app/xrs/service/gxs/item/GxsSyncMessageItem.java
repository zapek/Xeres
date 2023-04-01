package io.xeres.app.xrs.service.gxs.item;

import io.xeres.app.xrs.serialization.RsSerialized;
import io.xeres.common.id.GxsId;

public class GxsSyncMessageItem extends GxsExchange
{
	public static final byte REQUEST = 0x1;
	public static final byte RESPONSE = 0x2;

	@RsSerialized
	private byte flags;

	@RsSerialized
	private GxsId groupId;

	@RsSerialized
	private GxsId messageId;

	@RsSerialized
	private GxsId authorId;

	public GxsSyncMessageItem()
	{
		// Needed
	}
}
