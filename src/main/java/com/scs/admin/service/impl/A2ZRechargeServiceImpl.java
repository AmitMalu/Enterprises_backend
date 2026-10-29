package com.scs.admin.service.impl;

import com.scs.admin.entities.VendorCredentials;
import com.scs.admin.request.DthCustomerInfoRequest;
import com.scs.admin.request.MobileLookupRequest;
import com.scs.admin.request.RechargePlanRequest;
import com.scs.admin.request.RechargeRequest;
import com.scs.admin.response.*;
import com.scs.admin.service.RechargeVendorService;

public class A2ZRechargeServiceImpl implements RechargeVendorService {
    @Override
    public DthCustomerInfoResponse getDthCustomerInfo(DthCustomerInfoRequest request, VendorCredentials credentials) {
        return null;
    }

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
