package com.im.admin.service;

import com.im.admin.dto.RequestHeaderMetaDto;
import com.im.admin.request.CashfreeCreateOrderRequest;
import com.im.admin.request.ImPgCreateOrderRequest;
import com.im.admin.response.PgStatusResponse;
import com.im.admin.response.PgUnifiedOrderResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

/**
 * @author SHUBHAM KHOPADE
 */
public interface ImPGService {

    PgUnifiedOrderResponse createOrder(ImPgCreateOrderRequest request, RequestHeaderMetaDto requestHeaderMetaDtoMeta);

    PgStatusResponse checkStatus(String orderId);

    void handleCallback(Map<String, String> payload, HttpServletRequest request);


}
