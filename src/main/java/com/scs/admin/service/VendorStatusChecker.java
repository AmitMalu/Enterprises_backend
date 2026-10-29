package com.scs.admin.service;

import com.scs.admin.entities.VendorCredentials;
import com.scs.admin.response.PayoutStatusResponse;

public interface VendorStatusChecker {

    PayoutStatusResponse checkPayoutStatus(String orderId, VendorCredentials credentials);

}
