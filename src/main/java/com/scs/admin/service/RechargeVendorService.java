package com.scs.admin.service;

import com.scs.admin.entities.VendorCredentials;
import com.scs.admin.request.DthCustomerInfoRequest;
import com.scs.admin.request.MobileLookupRequest;
import com.scs.admin.request.RechargePlanRequest;
import com.scs.admin.request.RechargeRequest;
import com.scs.admin.response.*;

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

    DthCustomerInfoResponse getDthCustomerInfo(
            DthCustomerInfoRequest request,
            VendorCredentials credentials);
}
