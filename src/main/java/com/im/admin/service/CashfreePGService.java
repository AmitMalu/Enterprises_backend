package com.im.admin.service;

import com.im.admin.request.CashfreeCreateOrderRequest;
import com.im.admin.request.CashfreeOrderPayRequest;
import com.im.admin.request.RazorPayCreateOrderRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.PgUnifiedOrderResponse;
import org.springframework.http.ResponseEntity;

/**
 * @author SHUBHAM KHOPADE
 */
public interface CashfreePGService {

    PgUnifiedOrderResponse createOrder(CashfreeCreateOrderRequest request, Double latitude, Double longitude);

    void processWebhook(String payload, String idempotencyKey);

}
