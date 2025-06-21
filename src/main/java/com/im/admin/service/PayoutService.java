package com.im.admin.service;

import com.im.admin.request.PayoutMerchantCallbackRequest;
import com.im.admin.request.SendPayoutRequest;
import com.im.admin.response.PayoutMerchantCallbackResponse;

import java.util.Map;

public interface PayoutService {

    String generateToken();

    String sendPayoutRequest(SendPayoutRequest sendPayoutRequest);

    PayoutMerchantCallbackResponse updateMerchantCallbackUrl(PayoutMerchantCallbackRequest request);

    String handlePayoutCallback(Map<String, Object> payload);

}
