package com.im.admin.service;

import com.im.admin.request.NixaSoftCallbackRequest;
import com.im.admin.request.RazorPayCreateOrderRequest;
import com.razorpay.RazorpayException;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author SHUBHAM KHOPADE
 */
public interface RazorPayPGService {

    String createOrder(RazorPayCreateOrderRequest request, Double latitude, Double longitude);

    void processCallback(String paymentId, String plinkId, String orderId, String status, String signature) throws RazorpayException;

}