package com.im.admin.service;

import com.im.admin.entities.VendorCredentials;
import com.im.admin.response.PayoutStatusResponse;

public interface VendorStatusChecker {

    PayoutStatusResponse checkPayoutStatus(String orderId, VendorCredentials credentials);

}
