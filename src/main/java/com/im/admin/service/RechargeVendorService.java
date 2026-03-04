package com.im.admin.service;

import com.im.admin.entities.VendorCredentials;
import com.im.admin.request.RechargeRequest;
import com.im.admin.response.RechargeResponse;

public interface RechargeVendorService {

    String getVendorCode();

    RechargeResponse recharge(RechargeRequest request,
                              VendorCredentials credentials);

    String checkBalance(RechargeRequest request, VendorCredentials credentials);

    String checkStatus(RechargeRequest request, VendorCredentials credentials);

    String checkStatusByTxnRef(String transactionRef,
                               VendorCredentials credentials);
}
