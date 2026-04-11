package com.im.admin.service;

import com.im.admin.dto.NotificationRequestDto;
import com.im.admin.entities.AlertType;
import com.im.admin.entities.Notification;
import com.im.admin.entities.NotificationMaster;

import java.util.List;

public interface NotificationService {
    NotificationMaster createNotification(NotificationMaster request);
//    Notification createNotification(NotificationRequestDto dto);

    List<AlertType> getAllAlertTypes();


    List<NotificationMaster> getAllNotifications();

    NotificationMaster getNotificationById(Long id);

    NotificationMaster updateNotification(Long id, NotificationMaster request);
}
