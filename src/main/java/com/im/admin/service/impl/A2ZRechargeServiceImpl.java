package com.im.admin.service.impl;

import com.im.admin.entities.VendorCredentials;
import com.im.admin.request.MobileLookupRequest;
import com.im.admin.request.RechargePlanRequest;
import com.im.admin.request.RechargeRequest;
import com.im.admin.response.MobileLookupResponse;
import com.im.admin.response.OperatorResponse;
import com.im.admin.response.RechargePlanResponse;
import com.im.admin.response.RechargeResponse;
import com.im.admin.service.RechargeVendorService;

public class A2ZRechargeServiceImpl implements RechargeVendorService {
    @Override
    public OperatorResponse getOperatorCodes(VendorCredentials credentials) {
        return null;
    }

    @Override
    public RechargePlanResponse getRechargePlans(RechargePlanRequest request, VendorCredentials credentials) {
        return null;
    }



    @Override
    public MobileLookupResponse mobileLookup(MobileLookupRequest request, VendorCredentials credentials) {
        return null;
    }

    @Override
    public String getVendorCode() {
        return "";
    }

    @Override
    public RechargeResponse recharge(RechargeRequest request,String orderId, VendorCredentials credentials) {
        return null;
    }


    @Override
    public String checkBalance(RechargeRequest request, VendorCredentials credentials) {
        return "";
    }

    @Override
    public String checkStatus(RechargeRequest request, VendorCredentials credentials) {
        return "";
    }

    @Override
    public String checkStatusByTxnRef(String transactionRef, VendorCredentials credentials) {
        return "";
    }
}
