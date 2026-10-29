package com.scs.admin.service;

import com.scs.admin.dto.RequestHeaderMetaDto;
import com.scs.admin.request.UnisusPayCreateOrderRequest;
import com.scs.admin.request.UnisusWebhookRequest;
import com.scs.admin.response.UnisusWebhookResponse;

public interface UnisusPGService {

    String authenticate(UnisusPayCreateOrderRequest request, RequestHeaderMetaDto requestHeaderMetaDto);

    UnisusWebhookResponse processWebhook(UnisusWebhookRequest request);
}
