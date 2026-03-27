package com.im.admin.service;

import com.im.admin.request.NixaSoftCallbackRequest;
import com.im.admin.request.RazorPayCreateOrderRequest;

/**
 * @author SHUBHAM KHOPADE
 */
public interface RazorPayPGService {

    String createOrder(RazorPayCreateOrderRequest request, Double latitude, Double longitude);

    void processCallback(NixaSoftCallbackRequest request);


}
