package com.im.admin.service;


import com.im.admin.response.DropdownResponseDto;
import com.im.admin.dto.NotificationRequestDto;
import com.im.admin.entities.AlertType;
import com.im.admin.entities.Notification;
import com.im.admin.entities.NotificationMaster;
import com.im.admin.response.NotificationResponseDto;
import org.springframework.data.domain.Page;


import java.util.List;

public interface NotificationService {
    NotificationResponseDto createNotification(NotificationRequestDto requestDto);//    Notification createNotification(NotificationRequestDto dto);

    List<AlertType> getAllAlertTypes();


    Page<NotificationResponseDto> getAllNotifications(int page, int size, String sortBy, String sortDir);    NotificationResponseDto getNotificationById(Long id);
    NotificationResponseDto updateNotification(Long id, NotificationRequestDto requestDto);
    DropdownResponseDto getDropdownData();
}
