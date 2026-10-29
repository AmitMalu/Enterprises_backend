package com.scs.admin.service;

import com.scs.admin.dto.RequestHeaderMetaDto;
import com.scs.admin.request.ShadvalPayCreateOrderRequest;

/**
 * @author SHUBHAM KHOPADE
 */
public interface ShadvalPGService {

    String createOrder(ShadvalPayCreateOrderRequest payCreateOrderRequest, RequestHeaderMetaDto requestHeaderMetaDto);

}
