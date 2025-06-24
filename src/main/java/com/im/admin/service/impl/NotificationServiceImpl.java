package com.im.admin.service.impl;

import com.im.admin.dto.NotificationRequestDto;
import com.im.admin.entities.Notification;
import com.im.admin.repositories.NotificationRepository;
import com.im.admin.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Override
    public Notification createNotification(NotificationRequestDto dto) {
        Notification notification = new Notification();
        notification.setMessage(dto.getMessage());
        notification.setType(dto.getType());
        notification.setVisible(dto.getVisible());
        notification.setCreatedBy(dto.getCreatedBy());
        notification.setHierarchyCodes(dto.getHierarchyCodes());

        return notificationRepository.save(notification);

    }
}
