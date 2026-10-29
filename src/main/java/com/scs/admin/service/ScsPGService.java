package com.scs.admin.service;

import com.scs.admin.dto.RequestHeaderMetaDto;
import com.scs.admin.request.CashfreeCreateOrderRequest;
import com.scs.admin.request.ScsCallbackRequest;
import com.scs.admin.request.ScsPgCreateOrderRequest;
import com.scs.admin.response.PgStatusResponse;
import com.scs.admin.response.PgUnifiedOrderResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

/**
 * @author SHUBHAM KHOPADE
 */
public interface ScsPGService {

    PgUnifiedOrderResponse createOrder(ScsPgCreateOrderRequest request, RequestHeaderMetaDto requestHeaderMetaDtoMeta);

    PgStatusResponse checkStatus(String orderId);

    void handleCallback(ScsCallbackRequest scsCallbackRequest, HttpServletRequest request);


}
