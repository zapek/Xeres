package io.xeres.common.rest.forum;

public record PostRequest(
		long forumId,
		long originalId,
		long replyToId
)
{
	@Override
	public String toString()
	{
		// This is used by the Window Manager to find the window by its unique title
		return forumId + "," + originalId + "," + replyToId;
	}
}
