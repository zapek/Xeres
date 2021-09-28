package io.xeres.app.xrs.item;

public enum ItemPriority
{
	DEFAULT(3),
	GXS(6),
	CHAT(7);
	// XXX: add other priorities here, find good names. does RTT really have default?, etc...

	private final int priority;

	ItemPriority(int priority)
	{
		this.priority = priority;
	}

	public int getPriority()
	{
		return priority;
	}
}

