package com.scs.admin.service;

import com.scs.admin.dto.RequestHeaderMetaDto;
import com.scs.admin.request.NixaSoftCallbackRequest;
import com.scs.admin.request.NixaSoftCreateOrderRequest;

/**
 * @author SHUBHAM KHOPADE
 */
public interface NixaSoftPGService {

    String createOrder(NixaSoftCreateOrderRequest nixaSoftCreateOrderRequest, RequestHeaderMetaDto requestHeaderMetaDto);

    void processCallback(NixaSoftCallbackRequest request);

    void checkStatus(String orderId);

}
