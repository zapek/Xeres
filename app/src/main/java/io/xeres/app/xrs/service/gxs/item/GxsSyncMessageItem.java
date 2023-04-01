package io.xeres.app.xrs.service.gxs.item;

import io.xeres.app.xrs.serialization.RsSerialized;
import io.xeres.common.id.GxsId;

public class GxsSyncMessageItem extends GxsExchange
{
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
