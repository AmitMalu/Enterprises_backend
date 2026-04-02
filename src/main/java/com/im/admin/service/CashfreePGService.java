package com.im.admin.service;

import com.im.admin.request.CashfreeCreateOrderRequest;
import com.im.admin.request.CashfreeOrderPayRequest;
import com.im.admin.request.RazorPayCreateOrderRequest;

/**
 * @author SHUBHAM KHOPADE
 */
public interface CashfreePGService {

    String createOrder(CashfreeCreateOrderRequest request, Double latitude, Double longitude);

    void processWebhook(String payload,
                        String signature,
                        String timestamp,
                        String idempotencyKey);

}
