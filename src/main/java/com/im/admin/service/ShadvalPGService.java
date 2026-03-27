package com.im.admin.service;

import com.im.admin.request.ShadvalPayCreateOrderRequest;

/**
 * @author SHUBHAM KHOPADE
 */
public interface ShadvalPGService {

    String createOrder(ShadvalPayCreateOrderRequest payCreateOrderRequest, Double latitude, Double longitude);

}
