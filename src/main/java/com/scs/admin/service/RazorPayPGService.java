package com.scs.admin.service;

import com.scs.admin.dto.RequestHeaderMetaDto;
import com.scs.admin.request.NixaSoftCallbackRequest;
import com.scs.admin.request.RazorPayCreateOrderRequest;
import com.razorpay.RazorpayException;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author SHUBHAM KHOPADE
 */
public interface RazorPayPGService {

    String createOrder(RazorPayCreateOrderRequest request, RequestHeaderMetaDto requestHeaderMetaDto);

    void processCallback(String paymentId, String plinkId, String orderId, String status, String signature) throws RazorpayException;

}