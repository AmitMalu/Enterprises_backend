package com.scs.admin.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

// For handling inbound callbacks from vendor
public interface PayoutCallbackHandler {

    String handleCallback(Map<String, String> payload, HttpServletRequest request) throws JsonProcessingException;

}
