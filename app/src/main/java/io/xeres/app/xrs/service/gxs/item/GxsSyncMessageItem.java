package io.xeres.app.xrs.service.gxs.item;

import io.xeres.app.xrs.serialization.FieldSize;
import io.xeres.app.xrs.serialization.RsSerialized;
import io.xeres.common.id.GxsId;

import java.util.Set;

public class GxsSyncMessageItem extends GxsExchange
{
	@RsSerialized(fieldSize = FieldSize.BYTE)
	private Set<SyncFlags> flags;

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
