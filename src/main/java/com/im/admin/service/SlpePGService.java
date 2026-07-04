package com.im.admin.service;

import com.im.admin.dto.RequestHeaderMetaDto;
import com.im.admin.request.SlpePgCreateOrderRequest;
import com.im.admin.request.UnisusPayCreateOrderRequest;
import com.im.admin.response.PgStatusResponse;
import org.springframework.http.ResponseEntity;

import java.util.Map;

/**
 * @author SHUBHAM KHOPADE
 */
public interface SlpePGService {

    String createOrder(SlpePgCreateOrderRequest request, RequestHeaderMetaDto requestHeaderMetaDto);

    void processCallback(Map<String, Object> payload);

    PgStatusResponse checkStatus(String orderId);


}
