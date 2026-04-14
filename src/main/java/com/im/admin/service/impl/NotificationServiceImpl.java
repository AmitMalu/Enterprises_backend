package com.im.admin.service.impl;

import com.im.admin.dto.NotificationRequestDto;
import com.im.admin.entities.AlertType;
import com.im.admin.entities.AudienceType;
import com.im.admin.entities.Notification;
import com.im.admin.entities.NotificationMaster;
import com.im.admin.entities.product.Products;
import com.im.admin.repositories.*;

import com.im.admin.response.DropdownOptionDto;
import com.im.admin.response.DropdownResponseDto;
import com.im.admin.response.NotificationResponseDto;
import com.im.admin.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationMasterRepository notificationMasterRepository;

    @Autowired
    private ProductsRepository productsRepository;

    @Autowired
    private AudienceTypeRepository audienceTypeRepository;

    @Autowired
    private AlertTypeRepository alertTypeRepository;

    @Autowired
    private WebSocketService webSocketService;



    @Override
    public NotificationResponseDto createNotification(NotificationRequestDto requestDto) {

        // VALIDATION
        if (requestDto.getAlertType() == null) {
            throw new RuntimeException("Alert Type required");
        }

        if (requestDto.getTitle() == null) {
            throw new RuntimeException("Title required");
        }

        if (requestDto.getAudienceType() == null) {
            throw new RuntimeException("Audience Type required");
        }

        //  CONVERT DTO → ENTITY
        NotificationMaster entity = new NotificationMaster();

        entity.setAlertType(requestDto.getAlertType());
//  Get Product by ID
        Products product = productsRepository.findById(
                Short.valueOf(requestDto.getProduct())   // assuming ID is Short
        ).orElseThrow(() -> new RuntimeException("Product not found"));

//  Set Product Name
        entity.setProduct(product.getName());
        entity.setTitle(requestDto.getTitle());
        entity.setShortMessage(requestDto.getBody());
        entity.setDeliveryChannel(requestDto.getDeliveryChannel());
        entity.setAudienceType(requestDto.getAudienceType());
        entity.setSeverity(requestDto.getSeverity());
        entity.setSendMode(requestDto.getSendMode());
        entity.setRole(requestDto.getRole());
        entity.setHierarchyId(requestDto.getHierarchyId());
        entity.setCreatedAt(LocalDateTime.now());

        // optional schedule time
        if ("SCHEDULE".equalsIgnoreCase(requestDto.getSendMode())
                && requestDto.getScheduleTime() != null) {

            entity.setScheduleTime(requestDto.getScheduleTime());
        }

        //  SAVE
        NotificationMaster saved = notificationMasterRepository.save(entity);

        //  PAYLOAD FOR WEBSOCKET
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", saved.getId());
        payload.put("title", saved.getTitle());
        payload.put("message", saved.getShortMessage());
        payload.put("type", saved.getAlertType());
        payload.put("createdAt", saved.getCreatedAt());

        // TARGETING LOGIC
        if ("ALL".equalsIgnoreCase(saved.getAudienceType())) {

            webSocketService.sendToAll(payload);

        } else if ("ROLE".equalsIgnoreCase(saved.getAudienceType())) {

            webSocketService.sendToRole(saved.getRole(), payload);

        } else if ("USER".equalsIgnoreCase(saved.getAudienceType())) {

            webSocketService.sendToUser(saved.getHierarchyId(), payload);
        }

        //  ENTITY → RESPONSE DTO
        return NotificationResponseDto.builder()
                .id(saved.getId())
                .alertType(saved.getAlertType())
                .product(saved.getProduct())
                .title(saved.getTitle())
                .body(saved.getShortMessage())
                .deliveryChannel(saved.getDeliveryChannel())
                .audienceType(saved.getAudienceType())
                .severity(saved.getSeverity())
                .sendMode(saved.getSendMode())
                .scheduleTime(saved.getScheduleTime())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Override
    public List<AlertType> getAllAlertTypes() {
        return alertTypeRepository.findAll();
    }

    @Override
    public Page<NotificationResponseDto> getAllNotifications(int page, int size, String sortBy, String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() :
                Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<NotificationMaster> pageData =
                notificationMasterRepository.findAll(pageable);

        return pageData.map(n -> NotificationResponseDto.builder()
                .id(n.getId())
                .alertType(n.getAlertType())
                .product(n.getProduct())
                .title(n.getTitle())
                .body(n.getShortMessage())
                .deliveryChannel(n.getDeliveryChannel())
                .audienceType(n.getAudienceType())
                .severity(n.getSeverity())
                .sendMode(n.getSendMode())
                .scheduleTime(n.getScheduleTime())
                .createdAt(n.getCreatedAt())
                .build()
        );
    }

    @Override
    public NotificationResponseDto getNotificationById(Long id) {

        NotificationMaster n = notificationMasterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        return NotificationResponseDto.builder()
                .id(n.getId())
                .alertType(n.getAlertType())
                .product(n.getProduct())
                .title(n.getTitle())
                .body(n.getShortMessage())
                .deliveryChannel(n.getDeliveryChannel())
                .audienceType(n.getAudienceType())
                .severity(n.getSeverity())
                .sendMode(n.getSendMode())
                .scheduleTime(n.getScheduleTime())
                .createdAt(n.getCreatedAt())
                .build();
    }

    @Override
    public NotificationResponseDto updateNotification(Long id, NotificationRequestDto requestDto) {

        NotificationMaster existing = notificationMasterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        //  UPDATE ONLY REQUIRED FIELDS (DTO based)
        existing.setAlertType(requestDto.getAlertType());
        existing.setProduct(requestDto.getProduct());
        existing.setTitle(requestDto.getTitle());
        existing.setShortMessage(requestDto.getBody());
        existing.setDeliveryChannel(requestDto.getDeliveryChannel());
        existing.setAudienceType(requestDto.getAudienceType());
        existing.setSeverity(requestDto.getSeverity());
        existing.setSendMode(requestDto.getSendMode());
        existing.setRole(requestDto.getRole());
        existing.setHierarchyId(requestDto.getHierarchyId());

        //  Schedule handling
        if ("SCHEDULE".equalsIgnoreCase(requestDto.getSendMode())
                && requestDto.getScheduleTime() != null) {

            existing.setScheduleTime(requestDto.getScheduleTime());
        }

        NotificationMaster updated = notificationMasterRepository.save(existing);

        //  PAYLOAD
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", updated.getId());
        payload.put("title", updated.getTitle());
        payload.put("message", updated.getShortMessage());
        payload.put("type", updated.getAlertType());
        payload.put("updatedAt", LocalDateTime.now());

        //  TARGETING
        if ("ALL".equalsIgnoreCase(updated.getAudienceType())) {

            webSocketService.sendToAll(payload);

        } else if ("ROLE".equalsIgnoreCase(updated.getAudienceType())) {

            webSocketService.sendToRole(updated.getRole(), payload);

        } else if ("USER".equalsIgnoreCase(updated.getAudienceType())) {

            webSocketService.sendToUser(updated.getHierarchyId(), payload);
        }

        //  ENTITY → RESPONSE DTO
        return NotificationResponseDto.builder()
                .id(updated.getId())
                .alertType(updated.getAlertType())
                .product(updated.getProduct())
                .title(updated.getTitle())
                .body(updated.getShortMessage())
                .deliveryChannel(updated.getDeliveryChannel())
                .audienceType(updated.getAudienceType())
                .severity(updated.getSeverity())
                .sendMode(updated.getSendMode())
                .scheduleTime(updated.getScheduleTime())
                .createdAt(updated.getCreatedAt())
                .build();
    }


    @Override
    public DropdownResponseDto getDropdownData() {

        // Alert Types
        List<DropdownOptionDto> alertTypes = alertTypeRepository.findAll()
                .stream()
                .map(a -> new DropdownOptionDto(
                        a.getAlertTypeName(),   // label
                        a.getAlertTypeName()    // value (same)
                ))
                .toList();

        // Products
        List<DropdownOptionDto> products = productsRepository.findAll()
                .stream()
                .map(p -> new DropdownOptionDto(
                        p.getName(),                  // label
                        String.valueOf(p.getId())     // value (ID)
                ))
                .toList();

        // Audience Types
        List<DropdownOptionDto> audienceTypes = audienceTypeRepository.findAll()
                .stream()
                .map(a -> new DropdownOptionDto(
                        a.getName(),                 // label
                       a.getName()
                ))
                .toList();

        return DropdownResponseDto.builder()
                .alertType(alertTypes)
                .audienceType(audienceTypes)
                .product(products)
                .build();
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
