package io.xeres.app.xrs.service.gxs.item;

import io.xeres.app.database.model.gxs.GxsMessageItem;
import io.xeres.app.xrs.serialization.RsSerialized;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.MessageId;

public class GxsSyncMessageItem extends GxsExchange
{
	public static final byte REQUEST = 0x1;
	public static final byte RESPONSE = 0x2;

	@RsSerialized
	private byte flags;

	@RsSerialized
	private GxsId groupId;

	@RsSerialized
	private MessageId messageId;

	@RsSerialized
	private GxsId authorId;

	public GxsSyncMessageItem()
	{
		// Needed
	}

	public GxsSyncMessageItem(byte flags, GxsMessageItem messageItem, int transactionId)
	{
		this.flags = flags;
		groupId = messageItem.getGxsId();
		messageId = messageItem.getMessageId();
		authorId = messageItem.getAuthorId();
		setTransactionId(transactionId);
	}
}
