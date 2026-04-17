package com.im.admin.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class WebSocketService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    //  Send to ALL users
//    public void sendToAll(Object payload) {
//
//        log.info(" Sending notification to ALL users");
//        log.debug("Payload: {}", payload);
//
//        messagingTemplate.convertAndSend(
//                "/topic/notifications/web/ALL",
//                payload
//        );
//
//        log.info(" Sent to /topic/notifications/ALL");
//    }
//
//
//    //  2. Send to specific ROLE (e.g. RT, ADMIN)
//    public void sendToRole(String role, Object payload) {
//
//        log.info(" Sending notification to ROLE: {}", role);
//        log.debug("Payload: {}", payload);
//
//        String destination = "/topic/notifications/web/role/" + role;
//
//        messagingTemplate.convertAndSend(destination, payload);
//
//        log.info(" Sent to {}", destination);
//    }
//
//
//    //  3. Send to specific USER (hierarchyId)
//    public void sendToUser(String hierarchyId, Object payload) {
//
//        log.info(" Sending notification to USER: {}", hierarchyId);
//        log.debug("Payload: {}", payload);
//
//        String destination = "/topic/notifications/web/user/" + hierarchyId;
//
//        messagingTemplate.convertAndSend(destination, payload);
//
//        log.info(" Sent to {}", destination);
//    }


    public void sendToAll(String channel, Object payload) {

        String destination = buildPrefix(channel) + "/ALL";

        log.info("Sending notification to ALL via {} channel", channel);
        log.debug("Destination: {}", destination);
        log.debug("Payload: {}", payload);

        messagingTemplate.convertAndSend(destination, payload);
    }

    //  ROLE
    public void sendToRole(String channel, String role, Object payload) {

        String destination = buildPrefix(channel) + "/role/" + role;

        log.info("Sending notification to ROLE: {} via {}", role, channel);
        log.debug("Destination: {}", destination);
        log.debug("Payload: {}", payload);

        messagingTemplate.convertAndSend(destination, payload);
    }

    //  USER
    public void sendToUser(String channel, String hierarchyId, Object payload) {

        String destination = buildPrefix(channel) + "/user/" + hierarchyId;

        log.info("Sending notification to USER: {} via {}", hierarchyId, channel);
        log.debug("Destination: {}", destination);
        log.debug("Payload: {}", payload);

        messagingTemplate.convertAndSend(destination, payload);
    }

    private String buildPrefix(String channel) {
        return "/topic/notifications/" + channel.toLowerCase();
    }

}