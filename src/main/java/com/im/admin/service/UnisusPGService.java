package com.im.admin.service;

import com.im.admin.dto.RequestHeaderMetaDto;
import com.im.admin.request.UnisusPayCreateOrderRequest;
import com.im.admin.request.UnisusWebhookRequest;
import com.im.admin.response.UnisusWebhookResponse;

public interface UnisusPGService {

    String authenticate(UnisusPayCreateOrderRequest request, RequestHeaderMetaDto requestHeaderMetaDto);

    UnisusWebhookResponse processWebhook(UnisusWebhookRequest request);
}
