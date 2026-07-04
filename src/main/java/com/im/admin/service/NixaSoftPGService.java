package com.im.admin.service;

import com.im.admin.dto.RequestHeaderMetaDto;
import com.im.admin.request.NixaSoftCallbackRequest;
import com.im.admin.request.NixaSoftCreateOrderRequest;

/**
 * @author SHUBHAM KHOPADE
 */
public interface NixaSoftPGService {

    String createOrder(NixaSoftCreateOrderRequest nixaSoftCreateOrderRequest, RequestHeaderMetaDto requestHeaderMetaDto);

    void processCallback(NixaSoftCallbackRequest request);

    void checkStatus(String orderId);

}
