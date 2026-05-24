package com.smartqueue.app.service;

import com.smartqueue.app.entity.Notification;
import java.util.List;

public interface NotificationService {
    Notification sendNotification(Long userId, String message);
    List<Notification> getNotificationsForUser(Long userId);
    void markAsRead(Long notificationId);
    void markAllAsRead(Long userId);
}
