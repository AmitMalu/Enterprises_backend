package com.scs.admin.service;

import com.scs.admin.dto.RequestHeaderMetaDto;
import com.scs.admin.request.SlpePgCreateOrderRequest;
import com.scs.admin.request.UnisusPayCreateOrderRequest;
import com.scs.admin.response.PgStatusResponse;
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
