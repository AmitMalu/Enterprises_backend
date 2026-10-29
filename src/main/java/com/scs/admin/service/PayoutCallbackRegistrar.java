package com.scs.admin.service;

import com.scs.admin.entities.VendorCredentials;
import com.scs.admin.response.PayoutMerchantCallbackResponse;

// For registering/updating callback URL (vendor that needs registration)
public interface PayoutCallbackRegistrar {

    PayoutMerchantCallbackResponse registerCallbackUrl(VendorCredentials creds, String callbackUrl);

}
