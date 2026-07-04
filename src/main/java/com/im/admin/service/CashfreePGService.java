package com.im.admin.service;

import com.im.admin.dto.RequestHeaderMetaDto;
import com.im.admin.request.CashfreeCreateOrderRequest;
import com.im.admin.request.CashfreeOrderPayRequest;
import com.im.admin.request.RazorPayCreateOrderRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.PgStatusResponse;
import com.im.admin.response.PgUnifiedOrderResponse;
import org.springframework.http.ResponseEntity;

/**
 * @author SHUBHAM KHOPADE
 */
public interface CashfreePGService {

    PgUnifiedOrderResponse createOrder(CashfreeCreateOrderRequest request, RequestHeaderMetaDto requestHeaderMetaDtoMeta);

    void processWebhook(String payload, String idempotencyKey);

    PgStatusResponse checkStatus(String orderId);

}
