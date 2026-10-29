package com.scs.admin.service.impl;

import com.scs.admin.dto.NotificationRequestDto;
import com.scs.admin.entities.*;
import com.scs.admin.entities.enums.EmailType;
import com.scs.admin.entities.product.Products;
import com.scs.admin.repositories.*;

import com.scs.admin.request.EmailCredentialsRequestDto;
import com.scs.admin.request.SmsCredentialsRequestDto;
import com.scs.admin.response.DropdownOptionDto;
import com.scs.admin.response.DropdownResponseDto;
import com.scs.admin.response.NotificationResponseDto;
import com.scs.admin.service.NotificationService;
import com.scs.admin.util.CommonMethods;
import com.scs.admin.util.TokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private TokenUtil tokenUtil;

    @Autowired
    private EmailCredentialsRepository emailCredentialsRepository;

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

    @Autowired
    private UserNotificationReadRepository userNotificationReadRepository;

    @Autowired
    private CommonMethods commonMethods;

    @Autowired
    private WhiteLabelRepository whiteLabelRepository;

    @Autowired
    private SmsCredentialsRepository smsCredentialsRepository;

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
        NotificationResponseDto payload = NotificationResponseDto.builder()
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
                .isRead(false) //  always false for new notification
                .build();

        List<String> channels;

        String deliveryChannel = Optional.ofNullable(saved.getDeliveryChannel())
                .orElse("WEB")
                .toUpperCase();

        if ("BOTH".equalsIgnoreCase(deliveryChannel)) {
            channels = List.of("WEB", "APP");
        } else {
            channels = List.of(deliveryChannel);
        }


        for (String channel : channels) {

            if ("ALL".equalsIgnoreCase(saved.getAudienceType())) {

                webSocketService.sendToAll(channel, payload);

            } else if (
                    "RT".equalsIgnoreCase(saved.getAudienceType()) ||
                            "DT".equalsIgnoreCase(saved.getAudienceType()) ||
                            "PT".equalsIgnoreCase(saved.getAudienceType()) ||
                            "ET".equalsIgnoreCase(saved.getAudienceType()) ||
                            "WL".equalsIgnoreCase(saved.getAudienceType())
            ) {

                webSocketService.sendToRole(channel, saved.getAudienceType(), payload);

            } else if ("USER".equalsIgnoreCase(saved.getAudienceType())) {

                webSocketService.sendToUser(channel, saved.getHierarchyId(), payload);
            }
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

        //  VALIDATION
        if (requestDto.getAlertType() == null) {
            throw new RuntimeException("Alert Type required");
        }

        if (requestDto.getTitle() == null) {
            throw new RuntimeException("Title required");
        }

        if (requestDto.getAudienceType() == null) {
            throw new RuntimeException("Audience Type required");
        }

        //  UPDATE FIELDS

        existing.setAlertType(requestDto.getAlertType());

        //  FIXED PRODUCT (IMPORTANT)
        Products product = productsRepository.findById(
                Short.valueOf(requestDto.getProduct())
        ).orElseThrow(() -> new RuntimeException("Product not found"));

        existing.setProduct(product.getName());

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
        } else {
            existing.setScheduleTime(null);
        }

        NotificationMaster updated = notificationMasterRepository.save(existing);

        //  PREPARE PAYLOAD
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", updated.getId());
        payload.put("title", updated.getTitle());
        payload.put("message", updated.getShortMessage());
        payload.put("type", updated.getAlertType());
        payload.put("createdAt", updated.getCreatedAt());
        payload.put("updatedAt", LocalDateTime.now());

        // CHANNEL (WEB / APP)
        String channel = Optional.ofNullable(updated.getDeliveryChannel())
                .orElse("WEB")
                .toUpperCase();

        //  TARGETING LOGIC (FIXED)
        if ("ALL".equalsIgnoreCase(updated.getAudienceType())) {

            webSocketService.sendToAll(channel, payload);

        } else if (
                "RT".equalsIgnoreCase(updated.getAudienceType()) ||
                        "DT".equalsIgnoreCase(updated.getAudienceType()) ||
                        "PT".equalsIgnoreCase(updated.getAudienceType()) ||
                        "ET".equalsIgnoreCase(updated.getAudienceType()) ||
                        "WL".equalsIgnoreCase(updated.getAudienceType())
        ) {

            //  audienceType itself is role
            webSocketService.sendToRole(channel, updated.getAudienceType(), payload);

        } else if ("USER".equalsIgnoreCase(updated.getAudienceType())) {

            webSocketService.sendToUser(channel, updated.getHierarchyId(), payload);
        }

        //  RESPONSE DTO
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

    private String getUserId() {
        return tokenUtil.extractHierarchyId(); //
    }


    @Override
    public Page<NotificationResponseDto> getUserNotifications(int page, int size) {

        String userId = getUserId();

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<NotificationMaster> notifications =
                notificationMasterRepository.findAll(pageable);

        List<UserNotificationRead> readList =
                userNotificationReadRepository.findByUserId(userId);

        Set<Long> readIds = readList.stream()
                .map(UserNotificationRead::getNotificationId)
                .collect(Collectors.toSet());

        return notifications.map(n -> NotificationResponseDto.builder()
                .id(n.getId())
                .title(n.getTitle())
                .body(n.getShortMessage())
                .alertType(n.getAlertType())
                .createdAt(n.getCreatedAt())
                .isRead(readIds.contains(n.getId()))
                .build()
        );
    }


    @Override
    public long getUnreadCount() {

        String userId = getUserId();

        long total = notificationMasterRepository.count();

        long read = userNotificationReadRepository.findByUserId(userId).size();

        return total - read;
    }


    @Override
    public void markAsRead(Long notificationId) {

        String userId = getUserId();

        boolean exists = userNotificationReadRepository
                .existsByUserIdAndNotificationId(userId, notificationId);

        if (!exists) {
            UserNotificationRead read = new UserNotificationRead();
            read.setUserId(userId);
            read.setNotificationId(notificationId);
            read.setReadAt(LocalDateTime.now());

            userNotificationReadRepository.save(read);
        }
    }

    @Override
    public void markAllAsRead() {

        String userId = getUserId();

        String currentType = commonMethods.extractHierarchyType(userId);

        if (currentType == null || currentType.isBlank()) {
            currentType = "ALL";
        }

        List<String> audienceTypes = List.of("ALL", currentType);

        List<NotificationMaster> notifications =
                notificationMasterRepository.findByAudienceTypeInIgnoreCase(audienceTypes);

        //  Fetch already read IDs in ONE query
        List<UserNotificationRead> readList =
                userNotificationReadRepository.findByUserId(userId);

        Set<Long> readIds = readList.stream()
                .map(UserNotificationRead::getNotificationId)
                .collect(Collectors.toSet());

        List<UserNotificationRead> newReads = new ArrayList<>();

        for (NotificationMaster n : notifications) {
            if (!readIds.contains(n.getId())) {
                UserNotificationRead read = new UserNotificationRead();
                read.setUserId(userId);
                read.setNotificationId(n.getId());
                read.setReadAt(LocalDateTime.now());

                newReads.add(read);
            }
        }

        userNotificationReadRepository.saveAll(newReads);
    }


    @Override
    public Page<NotificationResponseDto> getAllAudienceNotifications(
            int page,
            int size,
            String deliveryChannel) {

        String userId = getUserId();

        //  extract RT / DT / etc
        String currentType = commonMethods.extractHierarchyType(userId);

        if (currentType == null || currentType.isBlank()) {
            currentType = "ALL";
        }

        //  Audience filter
        List<String> audienceTypes = List.of("ALL", currentType);

        //  Delivery filter
        List<String> deliveryChannels;

        if ("WEB".equalsIgnoreCase(deliveryChannel)) {
            deliveryChannels = List.of("WEB", "BOTH");
        } else if ("APP".equalsIgnoreCase(deliveryChannel)) {
            deliveryChannels = List.of("APP", "BOTH");
        } else {
            deliveryChannels = List.of("WEB", "APP", "BOTH"); // fallback
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("createdAt").descending()
        );

        Page<NotificationMaster> notificationPage =
                notificationMasterRepository
                        .findByAudienceTypeInIgnoreCaseAndDeliveryChannelInIgnoreCase(
                                audienceTypes,
                                deliveryChannels,
                                pageable
                        );

        List<UserNotificationRead> readList =
                userNotificationReadRepository.findByUserId(userId);

        Set<Long> readIds = readList.stream()
                .map(UserNotificationRead::getNotificationId)
                .collect(Collectors.toSet());

        return notificationPage.map(n -> NotificationResponseDto.builder()
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
                .isRead(readIds.contains(n.getId()))
                .build()
        );
    }

    @Override
    public String saveEmailCredentials(EmailCredentialsRequestDto dto) {

        String hierarchyId = tokenUtil.extractHierarchyId();

        // WL check
        if (emailCredentialsRepository.existsByWhitelabelId(dto.getWhitelabelId())) {
            throw new RuntimeException("Whitelabel already mapped with email credentials");
        }

        // Email check
        if (emailCredentialsRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        EmailCredentialsMaster entity = new EmailCredentialsMaster();

        entity.setEmail(dto.getEmail());
        entity.setPassword(dto.getPassword());
        entity.setType(dto.getType());
        entity.setAdminId(hierarchyId);
        entity.setWhitelabelId(dto.getWhitelabelId());
        entity.setSmtpHost(dto.getSmtpHost());
        entity.setSmtpPort(dto.getSmtpPort());

        emailCredentialsRepository.save(entity);

        return "Email credentials saved successfully";
    }

    @Override
    public Page<EmailCredentialsMaster> getAllEmailCredentials(int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

        return emailCredentialsRepository.findAll(pageable);
    }

    @Override
    public String updateEmailCredentials(Long id, EmailCredentialsRequestDto dto) {

        EmailCredentialsMaster entity = emailCredentialsRepository
                .findById(id)
                .orElseThrow(() -> new RuntimeException("Email credentials not found"));

        // WL check (exclude current record)
        if (emailCredentialsRepository.existsByWhitelabelIdAndIdNot(dto.getWhitelabelId(), id)) {
            throw new RuntimeException("Whitelabel already mapped with email credentials");
        }

        // Email check (exclude current record)
        if (emailCredentialsRepository.existsByEmailAndIdNot(dto.getEmail(), id)) {
            throw new RuntimeException("Email already exists");
        }

        entity.setEmail(dto.getEmail());
        entity.setPassword(dto.getPassword());
        entity.setType(dto.getType());
        entity.setWhitelabelId(dto.getWhitelabelId());
        entity.setSmtpHost(dto.getSmtpHost());
        entity.setSmtpPort(dto.getSmtpPort());

        emailCredentialsRepository.save(entity);

        return "Updated Successfully";
    }

    @Override
    public EmailCredentialsMaster getEmailCredentialById(Long id) {

        return emailCredentialsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Email Credential not found with id: " + id));
    }

    @Override
    public List<DropdownOptionDto> getWhiteLabelDropdownFromToken() {

        String hierarchyId = tokenUtil.extractHierarchyId();


        List<WhiteLabels> list = whiteLabelRepository.findByParentAccount(hierarchyId);

        return list.stream()
                .map(wl -> new DropdownOptionDto(
                        wl.getWhiteLabelId(),      // label
                        wl.getWhiteLabelId()    // value
                ))
                .toList();
    }


    @Override
    public String saveSmsCredentials(SmsCredentialsRequestDto dto) {

        String adminId = tokenUtil.extractHierarchyId();

        // WL check
//        if (smsCredentialsRepository.existsByWhitelabelId(dto.getWhitelabelId())) {
//            throw new RuntimeException("Whitelabel already mapped with SMS credentials");
//        }

        if (smsCredentialsRepository.existsByWhitelabelIdAndType(
                dto.getWhitelabelId(),
                dto.getType()
        )) {
            throw new RuntimeException("SMS credentials already exist for this type and whitelabel");
        }

        // Username check
//        if (smsCredentialsRepository.existsByUsername(dto.getUsername())) {
//            throw new RuntimeException("Username already exists");
//        }

        SmsCredentialsMaster entity = new SmsCredentialsMaster();

        entity.setBaseUrl(dto.getBaseUrl());
        entity.setUsername(dto.getUsername());
        entity.setPassword(dto.getPassword());
        entity.setSenderId(dto.getSenderId());
        entity.setPeId(dto.getPeId());
        entity.setDltTemplateId(dto.getDltTemplateId());
        entity.setRoute(dto.getRoute());
        entity.setType(dto.getType());

        entity.setAdminId(adminId);
        entity.setWhitelabelId(dto.getWhitelabelId());

        entity.setCreatedOn(LocalDateTime.now());
        entity.setCreatedBy(adminId);

        smsCredentialsRepository.save(entity);

        return "SMS credentials saved successfully";
    }


    @Override
    public Page<SmsCredentialsMaster> getAllSmsCredentials(int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

        return smsCredentialsRepository.findAll(pageable);
    }

    @Override
    public String updateSmsCredentials(Long id, SmsCredentialsRequestDto dto) {

        SmsCredentialsMaster entity = smsCredentialsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("SMS credentials not found"));

//        if (smsCredentialsRepository.existsByWhitelabelIdAndIdNot(dto.getWhitelabelId(), id)) {
//            throw new RuntimeException("Whitelabel already mapped with SMS credentials");
//        }

        if (smsCredentialsRepository.existsByWhitelabelIdAndType(
                dto.getWhitelabelId(),
                dto.getType()
        )) {
            throw new RuntimeException("SMS credentials already exist for this type and whitelabel");
        }


//        if (smsCredentialsRepository.existsByUsernameAndIdNot(dto.getUsername(), id)) {
//            throw new RuntimeException("Username already exists");
//        }

        entity.setBaseUrl(dto.getBaseUrl());
        entity.setUsername(dto.getUsername());
        entity.setPassword(dto.getPassword());
        entity.setSenderId(dto.getSenderId());
        entity.setPeId(dto.getPeId());
        entity.setDltTemplateId(dto.getDltTemplateId());
        entity.setRoute(dto.getRoute());
        entity.setWhitelabelId(dto.getWhitelabelId());
        entity.setType(dto.getType());

        entity.setEditedOn(LocalDateTime.now());
        entity.setEditedBy(tokenUtil.extractHierarchyId());

        smsCredentialsRepository.save(entity);

        return "SMS credentials updated successfully";
    }

    @Override
    public SmsCredentialsMaster getSmsCredentialById(Long id) {

        return smsCredentialsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("SMS Credential not found with id: " + id));
    }

    @Override
    public List<DropdownOptionDto> getSmsTypeDropdown() {

        return Arrays.stream(EmailType.values())
                .map(type -> new DropdownOptionDto(
                        type.name(),   // label
                        type.name()    // value
                ))
                .collect(Collectors.toList());
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

