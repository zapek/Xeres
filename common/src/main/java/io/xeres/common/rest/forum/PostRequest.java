package io.xeres.common.rest.forum;

public record PostRequest(
		long forumId,
		long parentId,
		long originalId
)
{
}
