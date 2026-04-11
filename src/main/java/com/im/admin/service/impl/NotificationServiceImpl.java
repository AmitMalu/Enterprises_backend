package com.im.admin.service.impl;

import com.im.admin.dto.NotificationRequestDto;
import com.im.admin.entities.AlertType;
import com.im.admin.entities.Notification;
import com.im.admin.entities.NotificationMaster;
import com.im.admin.repositories.AlertTypeRepository;
import com.im.admin.repositories.NotificationMasterRepository;
import com.im.admin.repositories.NotificationRepository;
import com.im.admin.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationMasterRepository notificationMasterRepository;

    @Autowired
    private AlertTypeRepository alertTypeRepository;

    @Autowired
    private WebSocketService webSocketService;



    @Override
    public NotificationMaster createNotification(NotificationMaster request) {

        //  Validation
        if (request.getNotificationName() == null) {
            throw new RuntimeException("Notification Name required");
        }

        if (request.getAlertType() == null) {
            throw new RuntimeException("Alert Type required");
        }

        if (request.getTitle() == null) {
            throw new RuntimeException("Title required");
        }

        request.setCreatedAt(LocalDateTime.now());

        //  Save in DB
        NotificationMaster saved = notificationMasterRepository.save(request);

        //  Prepare payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", saved.getId());
        payload.put("title", saved.getTitle());
        payload.put("message", saved.getShortMessage());
        payload.put("type", saved.getAlertType());
        payload.put("createdAt", saved.getCreatedAt());

        // MAIN LOGIC (TARGETING)

        if ("ALL".equalsIgnoreCase(request.getAudienceType())) {

            webSocketService.sendToAll(payload);

        } else if ("ROLE".equalsIgnoreCase(request.getAudienceType())) {

            // make sure request.getRole() is coming from UI
            webSocketService.sendToRole(request.getRole(), payload);

        } else if ("USER".equalsIgnoreCase(request.getAudienceType())) {

            // make sure hierarchyId is coming from UI
            webSocketService.sendToUser(request.getHierarchyId(), payload);
        }

        return saved;
    }

    @Override
    public List<AlertType> getAllAlertTypes() {
        return alertTypeRepository.findAll();
    }

    @Override
    public List<NotificationMaster> getAllNotifications() {
        return notificationMasterRepository.findAll();
    }

    @Override
    public NotificationMaster getNotificationById(Long id) {

        return notificationMasterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
    }

    @Override
    public NotificationMaster updateNotification(Long id, NotificationMaster request) {

        NotificationMaster existing = notificationMasterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        //  Update fields
        existing.setNotificationName(request.getNotificationName());
        existing.setInternalCode(request.getInternalCode());
        existing.setAlertType(request.getAlertType());
        existing.setProduct(request.getProduct());
        existing.setTitle(request.getTitle());
        existing.setShortMessage(request.getShortMessage());
        existing.setLongDescription(request.getLongDescription());
        existing.setDeliveryChannel(request.getDeliveryChannel());
        existing.setPlatform(request.getPlatform());
        existing.setAudienceType(request.getAudienceType());
        existing.setSeverity(request.getSeverity());
        existing.setCtaType(request.getCtaType());
        existing.setCtaLabel(request.getCtaLabel());
        existing.setCtaAction(request.getCtaAction());
        existing.setSendMode(request.getSendMode());
        existing.setScheduleTime(request.getScheduleTime());
        existing.setRequireApproval(request.getRequireApproval());
        existing.setApprovalGroup(request.getApprovalGroup());
        existing.setTrackClick(request.getTrackClick());

        NotificationMaster updated = notificationMasterRepository.save(existing);

        //  Prepare payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", updated.getId());
        payload.put("title", updated.getTitle());
        payload.put("message", updated.getShortMessage());
        payload.put("type", updated.getAlertType());
        payload.put("updatedAt", LocalDateTime.now());

        //  TARGETING LOGIC

        if ("ALL".equalsIgnoreCase(updated.getAudienceType())) {

            webSocketService.sendToAll(payload);

        } else if ("ROLE".equalsIgnoreCase(updated.getAudienceType())) {

            webSocketService.sendToRole(updated.getRole(), payload);

        } else if ("USER".equalsIgnoreCase(updated.getAudienceType())) {

            webSocketService.sendToUser(updated.getHierarchyId(), payload);
        }

        return updated;
    }


//    @Override
//    public Notification createNotification(NotificationRequestDto dto) {
//        Notification notification = new Notification();
//        notification.setMessage(dto.getMessage());
//        notification.setType(dto.getType());
//        notification.setVisible(dto.getVisible());
//        notification.setCreatedBy(dto.getCreatedBy());
//        notification.setHierarchyCodes(dto.getHierarchyCodes());
//
//        return notificationRepository.save(notification);
//
//    }
}
