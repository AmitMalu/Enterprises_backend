package com.im.admin.service;


import com.im.admin.entities.*;
import com.im.admin.request.EmailCredentialsRequestDto;
import com.im.admin.request.SmsCredentialsRequestDto;
import com.im.admin.response.DropdownOptionDto;
import com.im.admin.response.DropdownResponseDto;
import com.im.admin.dto.NotificationRequestDto;
import com.im.admin.response.NotificationResponseDto;
import org.springframework.data.domain.Page;


import java.util.List;

public interface NotificationService {
    NotificationResponseDto createNotification(NotificationRequestDto requestDto);//    Notification createNotification(NotificationRequestDto dto);

    List<AlertType> getAllAlertTypes();


    Page<NotificationResponseDto> getAllNotifications(int page, int size, String sortBy, String sortDir);

    NotificationResponseDto getNotificationById(Long id);

    NotificationResponseDto updateNotification(Long id, NotificationRequestDto requestDto);

    DropdownResponseDto getDropdownData();

    Page<NotificationResponseDto> getUserNotifications(int page, int size);

    long getUnreadCount();

    void markAsRead(Long notificationId);

    void markAllAsRead();

    Page<NotificationResponseDto> getAllAudienceNotifications(
            int page,
            int size,
            String deliveryChannel
    );

    String saveEmailCredentials(EmailCredentialsRequestDto dto);

    Page<EmailCredentialsMaster> getAllEmailCredentials(int page, int size);

    String updateEmailCredentials(Long id, EmailCredentialsRequestDto dto);

    EmailCredentialsMaster getEmailCredentialById(Long id);

    List<DropdownOptionDto> getWhiteLabelDropdownFromToken();

    String saveSmsCredentials(SmsCredentialsRequestDto dto);

    Page<SmsCredentialsMaster> getAllSmsCredentials(int page, int size);

    String updateSmsCredentials(Long id, SmsCredentialsRequestDto dto);

    SmsCredentialsMaster getSmsCredentialById(Long id);

    List<DropdownOptionDto> getSmsTypeDropdown();
}