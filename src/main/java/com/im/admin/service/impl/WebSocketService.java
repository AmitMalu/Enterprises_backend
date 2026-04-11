package com.im.admin.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class WebSocketService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    //  Send to ALL users
    public void sendToAll(Object payload) {
        messagingTemplate.convertAndSend(
                "/topic/notifications/ALL",
                payload
        );
    }

    //  2. Send to specific ROLE (e.g. RT, ADMIN)
    public void sendToRole(String role, Object payload) {
        messagingTemplate.convertAndSend(
                "/topic/notifications/role/" + role,
                payload
        );
    }

    //  3. Send to specific USER (hierarchyId)
    public void sendToUser(String hierarchyId, Object payload) {
        messagingTemplate.convertAndSend(
                "/topic/notifications/user/" + hierarchyId,
                payload
        );
    }
}