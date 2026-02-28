package com.im.admin.service;

import com.im.admin.request.UnisusPayCreateOrderRequest;
import com.im.admin.request.UnisusWebhookRequest;
import com.im.admin.response.UnisusWebhookResponse;

public interface UnisusPGService {

    String authenticate(UnisusPayCreateOrderRequest request);

    UnisusWebhookResponse processWebhook(UnisusWebhookRequest request);
}
