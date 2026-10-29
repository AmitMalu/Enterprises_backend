package com.scs.admin.service;

import com.scs.admin.dto.RequestHeaderMetaDto;
import com.scs.admin.request.CashfreeCreateOrderRequest;
import com.scs.admin.request.CashfreeOrderPayRequest;
import com.scs.admin.request.RazorPayCreateOrderRequest;
import com.scs.admin.response.CommonResponse;
import com.scs.admin.response.PgStatusResponse;
import com.scs.admin.response.PgUnifiedOrderResponse;
import org.springframework.http.ResponseEntity;

/**
 * @author SHUBHAM KHOPADE
 */
public interface CashfreePGService {

    PgUnifiedOrderResponse createOrder(CashfreeCreateOrderRequest request, RequestHeaderMetaDto requestHeaderMetaDtoMeta);

    void processWebhook(String payload, String idempotencyKey);

    PgStatusResponse checkStatus(String orderId);

}
