package io.xeres.common.rest.file;

public record FileProgress(String name, long currentSize, long totalSize, String hash)
{
}