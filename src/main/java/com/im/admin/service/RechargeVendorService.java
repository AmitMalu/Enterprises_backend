package com.im.admin.service;

import com.im.admin.entities.VendorCredentials;
import com.im.admin.request.MobileLookupRequest;
import com.im.admin.request.RechargePlanRequest;
import com.im.admin.request.RechargeRequest;
import com.im.admin.response.MobileLookupResponse;
import com.im.admin.response.OperatorResponse;
import com.im.admin.response.RechargePlanResponse;
import com.im.admin.response.RechargeResponse;

public interface RechargeVendorService {

    String getVendorCode();

    RechargeResponse recharge(RechargeRequest request,String orderId,
                              VendorCredentials credentials);

    String checkBalance(RechargeRequest request, VendorCredentials credentials);

    String checkStatus(RechargeRequest request, VendorCredentials credentials);

    String checkStatusByTxnRef(String transactionRef,
                               VendorCredentials credentials);


    MobileLookupResponse mobileLookup(MobileLookupRequest request,
                                      VendorCredentials credentials);

    OperatorResponse getOperatorCodes(VendorCredentials credentials);

    RechargePlanResponse getRechargePlans(RechargePlanRequest request,
                                          VendorCredentials credentials);
}
