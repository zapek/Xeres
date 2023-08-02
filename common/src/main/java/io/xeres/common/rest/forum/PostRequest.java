package io.xeres.common.rest.forum;

public record PostRequest(
		String forumId,
		String parentMessage
)
{
}
