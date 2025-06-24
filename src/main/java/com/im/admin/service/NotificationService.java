package com.im.admin.service;

import com.im.admin.dto.NotificationRequestDto;
import com.im.admin.entities.Notification;

public interface NotificationService {
    Notification createNotification(NotificationRequestDto dto);
}
