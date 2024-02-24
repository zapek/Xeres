package io.xeres.common.rest.notification.file;

import io.xeres.common.rest.notification.Notification;

public record FileNotification(FileNotificationAction action, String shareName, String scannedFile) implements Notification
{
}
