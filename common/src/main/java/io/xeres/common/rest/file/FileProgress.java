package io.xeres.common.rest.file;

public record FileProgress(long id, String name, long currentSize, long totalSize, String hash)
{
}