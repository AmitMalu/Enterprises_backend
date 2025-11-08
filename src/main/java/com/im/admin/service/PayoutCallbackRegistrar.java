package com.im.admin.service;

import com.im.admin.entities.VendorCredentials;
import com.im.admin.response.PayoutMerchantCallbackResponse;

// For registering/updating callback URL (vendor that needs registration)
public interface PayoutCallbackRegistrar {

    PayoutMerchantCallbackResponse registerCallbackUrl(VendorCredentials creds, String callbackUrl);

}
