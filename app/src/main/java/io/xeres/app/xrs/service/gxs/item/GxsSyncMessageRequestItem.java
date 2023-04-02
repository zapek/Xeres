package io.xeres.app.xrs.service.gxs.item;

import io.xeres.app.xrs.serialization.RsSerialized;
import io.xeres.common.id.GxsId;

import static io.xeres.app.xrs.serialization.TlvType.STR_HASH_SHA1;

/**
 * Item used to request messages from a peer's group.
 */
public class GxsSyncMessageRequestItem extends GxsExchange
{
	public static final byte USE_HASHED_GROUP_ID = 0x2;

	@RsSerialized
	private byte flags;

	@RsSerialized
	private int createSince; // how far back to sync data

	@RsSerialized(tlvType = STR_HASH_SHA1)
	private String syncHash;

	@RsSerialized
	private GxsId groupId;

	@RsSerialized
	private int lastUpdated;

	public GxsSyncMessageRequestItem()
	{
		// Needed
	}

	public int getCreateSince()
	{
		return createSince;
	}

	public void setCreateSince(int createSince)
	{
		this.createSince = createSince;
	}

	public String getSyncHash()
	{
		return syncHash;
	}

	public void setSyncHash(String syncHash)
	{
		this.syncHash = syncHash;
	}

	public GxsId getGroupId()
	{
		return groupId;
	}

	public void setGroupId(GxsId groupId)
	{
		this.groupId = groupId;
	}

	public int getLastUpdated()
	{
		return lastUpdated;
	}

	public void setLastUpdated(int lastUpdated)
	{
		this.lastUpdated = lastUpdated;
	}

	@Override
	public String toString()
	{
		return "GxsSyncMessageRequestItem{" +
				"flags=" + flags +
				", createSince=" + createSince +
				", syncHash='" + syncHash + '\'' +
				", groupId=" + groupId +
				", lastUpdated=" + lastUpdated +
				", super=" + super.toString() +
				'}';
	}
}
